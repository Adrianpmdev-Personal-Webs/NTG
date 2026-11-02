package com.adrianpmdev.lol.features.players.application;

import com.adrianpmdev.lol.features.players.domain.LolQueue;
import com.adrianpmdev.lol.features.players.infrastructure.LolAccount;
import com.adrianpmdev.lol.features.players.infrastructure.LolAccountRepository;
import com.adrianpmdev.lol.features.players.infrastructure.LolMatchRepository;
import com.adrianpmdev.lol.features.riot.RiotApiException;
import com.adrianpmdev.lol.features.riot.RiotClient;
import com.adrianpmdev.lol.features.riot.RiotModels;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Brings games from Riot to the database, one at a time on a single thread that sleeps while there is nothing to do, so
 * it never adds load or memory on top of the small server. Downloads started by an admin run one after another; what a
 * player is waiting for goes first, between one game and the next. A game seen for one player is stored for every linked
 * player in it, and a download that stops starts again where it was because stored games are skipped.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
public class LolSyncWorker {

    private static final Duration COOLDOWN = Duration.ofMinutes(5);
    private static final Duration MARGIN = Duration.ofHours(2);
    private static final int PAGE = 100;
    private static final int STORED_CHUNK = 500;
    private static final int MAX_ERROR_LENGTH = 300;
    private static final String INSERT = """
            insert into lol.matches (user_id, match_id, queue_id, played_at, duration_seconds, champion_id,
                champion_name, win, remake, kills, deaths, assists, cs, damage, gold, vision, champion_level, items,
                spell1, spell2)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (user_id, match_id) do nothing
            """;

    private final RiotClient riotClient;
    private final LolAccountRepository accountRepository;
    private final LolMatchRepository matchRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Instant seasonStart;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<UUID, CompletableFuture<Void>> refreshes = new LinkedHashMap<>();
    private final Deque<UUID> downloads = new ArrayDeque<>();
    private final Map<UUID, Progress> progress = new ConcurrentHashMap<>();
    private Thread thread;
    private UUID downloading;
    private boolean stopCurrent;

    /**
     * How far a download is.
     *
     * @param saved games stored in this run
     * @param total games to store in this run
     */
    public record Progress(int saved, int total) {
    }

    private static final class Stopped extends RuntimeException {
    }

    /**
     * Builds the worker.
     *
     * @param riotClient        Riot API
     * @param accountRepository accounts
     * @param matchRepository   games
     * @param jdbcTemplate      inserts games with plain SQL
     * @param seasonStart       first day of the season followed, as yyyy-MM-dd
     */
    public LolSyncWorker(RiotClient riotClient, LolAccountRepository accountRepository,
                         LolMatchRepository matchRepository, JdbcTemplate jdbcTemplate,
                         @Value("${lol.season-start:2026-01-08}") String seasonStart) {
        this.riotClient = riotClient;
        this.accountRepository = accountRepository;
        this.matchRepository = matchRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.seasonStart = LocalDate.parse(seasonStart).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * First moment of the season followed.
     *
     * @return the moment
     */
    public Instant seasonStart() {
        return seasonStart;
    }

    /**
     * Adds a download to the queue.
     *
     * @param userId player
     */
    public synchronized void download(UUID userId) {
        if (!userId.equals(downloading) && !downloads.contains(userId)) {
            downloads.addLast(userId);
            wake();
        }
    }

    /**
     * Takes a download out of the queue, or stops it after the current game.
     *
     * @param userId player
     */
    public synchronized void pause(UUID userId) {
        downloads.remove(userId);

        if (userId.equals(downloading)) {
            stopCurrent = true;
        }
    }

    /**
     * Where the download of a player is right now.
     *
     * @param userId player
     * @return QUEUED, PREPARING while it lists the games, LOADING while it stores them, or empty without a download
     */
    public synchronized Optional<String> phaseOf(UUID userId) {
        if (downloads.contains(userId)) {
            return Optional.of("QUEUED");
        }

        if (userId.equals(downloading)) {
            return Optional.of(progress.containsKey(userId) ? LolAccount.LOADING : "PREPARING");
        }

        return Optional.empty();
    }

    /**
     * How far the running download of a player is.
     *
     * @param userId player
     * @return its progress, if it knows it yet
     */
    public Optional<Progress> progressOf(UUID userId) {
        return Optional.ofNullable(progress.get(userId));
    }

    /**
     * Asks for the new games and the rank of a player, unless they were checked a moment ago or their games were never
     * downloaded.
     *
     * @param account the account
     * @return what finishes when the check does; already finished when no check is needed
     */
    public synchronized CompletableFuture<Void> refresh(LolAccount account) {
        boolean recent = account.getCheckedAt() != null
                && account.getCheckedAt().plus(COOLDOWN).isAfter(Instant.now());

        if (recent || !LolAccount.COMPLETE.equals(account.getState())) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = refreshes.computeIfAbsent(account.getUserId(),
                id -> new CompletableFuture<>());
        wake();

        return future;
    }

    /** Stops the thread with the application. */
    @PreDestroy
    public synchronized void stop() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void wake() {
        if (thread == null) {
            thread = new Thread(this::run, "lol-sync");
            thread.setDaemon(true);
            thread.start();
        }

        notifyAll();
    }

    private void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                UUID next = nextDownload();

                if (next != null) {
                    runDownload(next);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /** Serves what players wait for and sleeps until there is something to do. */
    private UUID nextDownload() throws InterruptedException {
        serveRefreshes();

        synchronized (this) {
            if (refreshes.isEmpty() && downloads.isEmpty()) {
                wait();
                return null;
            }

            downloading = downloads.pollFirst();
            stopCurrent = false;

            return downloading;
        }
    }

    private void serveRefreshes() {
        while (true) {
            UUID userId;
            CompletableFuture<Void> future;

            synchronized (this) {
                if (refreshes.isEmpty()) {
                    return;
                }

                userId = refreshes.keySet().iterator().next();
                future = refreshes.remove(userId);
            }

            try {
                accountRepository.findById(userId).ifPresent(account -> {
                    sync(account, true);
                    account.setLastError(null);
                    accountRepository.save(account);
                });
                future.complete(null);
            } catch (RuntimeException exception) {
                log.warn("The games of {} could not be updated: {}", userId, exception.getMessage());
                future.completeExceptionally(exception);
            }
        }
    }

    private void runDownload(UUID userId) {
        Optional<LolAccount> found = accountRepository.findById(userId);

        try {
            if (found.isPresent()) {
                LolAccount account = found.get();
                account.setState(LolAccount.LOADING);
                account.setLastError(null);
                accountRepository.save(account);
                sync(account, false);
                account.setState(LolAccount.COMPLETE);
                accountRepository.save(account);
            }
        } catch (Stopped stopped) {
            found.ifPresent(account -> changeState(userId, LolAccount.PENDING, null));
        } catch (RuntimeException exception) {
            log.warn("The download of {} stopped: {}", userId, exception.getMessage());
            changeState(userId, LolAccount.ERROR, exception.getMessage());
        } finally {
            progress.remove(userId);

            synchronized (this) {
                downloading = null;
            }
        }
    }

    private void changeState(UUID userId, String state, String error) {
        accountRepository.findById(userId).ifPresent(account -> {
            account.setState(state);
            account.setLastError(error == null || error.length() <= MAX_ERROR_LENGTH
                    ? error : error.substring(0, MAX_ERROR_LENGTH));
            accountRepository.save(account);
        });
    }

    /**
     * Stores the games missing since the last complete check and reads the rank. The mark of the check only moves when
     * everything went well, so a failure is repaired by the next one.
     */
    private void sync(LolAccount account, boolean interactive) {
        Instant startedAt = Instant.now();
        long startTime = (account.getSyncedUntil() == null ? seasonStart : account.getSyncedUntil()).getEpochSecond();
        Set<String> missing = new LinkedHashSet<>();

        for (LolQueue queue : LolQueue.ALL) {
            missing.addAll(missingIn(account, queue.id(), startTime, interactive));
        }

        Map<String, UUID> linked = accountRepository.findAll().stream()
                .collect(Collectors.toMap(LolAccount::getPuuid, LolAccount::getUserId));
        int saved = 0;

        if (!interactive) {
            progress.put(account.getUserId(), new Progress(0, missing.size()));
        }

        for (String matchId : missing) {
            if (!interactive) {
                checkStop(account.getUserId());
                progress.put(account.getUserId(), new Progress(saved, missing.size()));
                serveRefreshes();
            }

            riotClient.match(matchId, interactive).ifPresent(match -> store(matchId, match, linked));
            saved++;
        }

        List<RiotModels.Rank> ranks = riotClient.ranks(account.getPuuid(), interactive);

        riotClient.summoner(account.getPuuid(), interactive).ifPresent(summoner -> {
            account.setProfileIconId(summoner.profileIconId());
            account.setSummonerLevel(summoner.summonerLevel());
        });
        account.setRanks(objectMapper.valueToTree(ranks));
        account.setSyncedUntil(startedAt.minus(MARGIN));
        account.setCheckedAt(Instant.now());
        accountRepository.save(account);
    }

    private synchronized void checkStop(UUID userId) {
        if (stopCurrent && userId.equals(downloading)) {
            throw new Stopped();
        }
    }

    private List<String> missingIn(LolAccount account, int queueId, long startTime, boolean interactive) {
        List<String> ids = new ArrayList<>();
        List<String> page;

        do {
            page = riotClient.matchIds(account.getPuuid(), queueId, startTime, ids.size(), PAGE, interactive);
            ids.addAll(page);
        } while (page.size() == PAGE);

        Set<String> stored = new LinkedHashSet<>();

        for (int from = 0; from < ids.size(); from += STORED_CHUNK) {
            stored.addAll(matchRepository.findStored(account.getUserId(),
                    ids.subList(from, Math.min(from + STORED_CHUNK, ids.size()))));
        }

        ids.removeAll(stored);

        return ids;
    }

    private void store(String matchId, RiotModels.Match match, Map<String, UUID> linked) {
        RiotModels.Info info = match.info();
        Map<UUID, RiotModels.Participant> players = new HashMap<>();

        for (RiotModels.Participant participant : info.participants()) {
            UUID userId = linked.get(participant.puuid());

            if (userId != null) {
                players.put(userId, participant);
            }
        }

        players.forEach((userId, player) -> jdbcTemplate.update(INSERT, userId, matchId, info.queueId(),
                Timestamp.from(Instant.ofEpochMilli(info.gameCreation())), (int) info.gameDuration(),
                player.championId(), player.championName(), player.win(), player.gameEndedInEarlySurrender(),
                player.kills(), player.deaths(), player.assists(),
                player.totalMinionsKilled() + player.neutralMinionsKilled(), player.totalDamageDealtToChampions(),
                player.goldEarned(), player.visionScore(), player.champLevel(), itemsOf(player),
                player.summoner1Id(), player.summoner2Id()));
    }

    private static String itemsOf(RiotModels.Participant player) {
        return player.item0() + "," + player.item1() + "," + player.item2() + "," + player.item3() + ","
                + player.item4() + "," + player.item5() + "," + player.item6();
    }

    /**
     * Tells a player why their update failed.
     *
     * @param exception what failed
     * @return a text safe to show
     */
    static String reasonOf(Throwable exception) {
        return exception instanceof RiotApiException ? exception.getMessage() : "No se ha podido actualizar";
    }
}
