package com.adrianpmdev.lol.features.players.application;

import com.adrianpmdev.lol.LolModule;
import com.adrianpmdev.lol.features.players.api.LolResponses;
import com.adrianpmdev.lol.features.players.domain.LolQueue;
import com.adrianpmdev.lol.features.players.domain.LolSeasons;
import com.adrianpmdev.lol.features.players.infrastructure.LolAccount;
import com.adrianpmdev.lol.features.players.infrastructure.LolAccountRepository;
import com.adrianpmdev.lol.features.players.infrastructure.LolMatch;
import com.adrianpmdev.lol.features.players.infrastructure.LolMatchRepository;
import com.adrianpmdev.lol.features.players.infrastructure.LolSeasonRanks;
import com.adrianpmdev.lol.features.riot.DataDragon;
import com.adrianpmdev.lol.features.riot.RiotApiException;
import com.adrianpmdev.lol.features.riot.RiotClient;
import com.adrianpmdev.lol.features.riot.RiotModels;
import com.adrianpmdev.platform.error.ApiErrors;
import com.adrianpmdev.platform.port.CurrentUser;
import com.adrianpmdev.platform.port.UserDirectory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accounts, history and statistics of the players. Everything is read from the database; Riot is only asked when a
 * player's games are updated or an account is linked.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class LolPlayerService {

    private static final Pattern RIOT_ID = Pattern.compile("^\\s*([^#]{3,16})\\s*#\\s*([\\p{L}\\p{N}]{2,5})\\s*$");
    private static final long REFRESH_WAIT_SECONDS = 20;
    private static final int TOP_CHAMPIONS = 5;
    /** A download makes 80 calls every 2 minutes at most, so a game takes about this long when alone. */
    private static final int SECONDS_PER_GAME = 2;
    private static final String NOT_LINKED = "NOT_LINKED";
    private static final String INTERRUPTED = "INTERRUPTED";
    private static final String[] APEX = {"MASTER", "GRANDMASTER", "CHALLENGER"};
    private static final Map<String, String> TIERS = Map.of("IRON", "Hierro", "BRONZE", "Bronce", "SILVER", "Plata",
            "GOLD", "Oro", "PLATINUM", "Platino", "EMERALD", "Esmeralda", "DIAMOND", "Diamante", "MASTER", "Maestro",
            "GRANDMASTER", "Gran Maestro", "CHALLENGER", "Aspirante");
    private static final Map<String, String> RANKED = Map.of("RANKED_SOLO_5x5", "SoloQ", "RANKED_FLEX_SR", "Flex");

    private final LolAccountRepository accountRepository;
    private final LolMatchRepository matchRepository;
    private final LolSyncWorker worker;
    private final LolSeasons seasons;
    private final LolSeasonRanks seasonRanks;
    private final RiotClient riotClient;
    private final DataDragon dataDragon;
    private final CurrentUser currentUser;
    private final UserDirectory userDirectory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The players with a linked account.
     *
     * @return the players and what the caller linked
     */
    @Transactional(readOnly = true)
    public LolResponses.Players players() {
        List<LolAccount> accounts = accountRepository.findAll();
        Map<UUID, String> names = userDirectory.usernames(accounts.stream().map(LolAccount::getUserId).toList());
        UUID caller = caller();
        String myRiotId = accounts.stream().filter(account -> account.getUserId().equals(caller))
                .map(LolPlayerService::riotIdOf).findFirst().orElse(null);
        List<LolResponses.PlayerSummary> players = accounts.stream()
                .map(account -> summaryOf(account, names.get(account.getUserId())))
                .sorted(Comparator.comparing(LolResponses.PlayerSummary::username,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return new LolResponses.Players(caller, myRiotId, players);
    }

    /**
     * What is stored about a player.
     *
     * @param userId player
     * @param season short name of the season, or null for the current one
     * @return their ranks, statistics and last games in that season
     */
    public LolResponses.PlayerDetail detail(UUID userId, String season) {
        return detailOf(find(userId), season, false, null);
    }

    /**
     * Every season, for the admin screen.
     *
     * @return seasons, newest first
     */
    public List<LolResponses.AdminSeason> adminSeasons() {
        requireAdmin();

        String current = seasons.current().key();

        return seasons.all().reversed().stream()
                .map(season -> new LolResponses.AdminSeason(season.key(), season.label(), season.date().toString(),
                        season.key().equals(current)))
                .toList();
    }

    /**
     * Creates a season or changes the day it starts.
     *
     * @param key  short name
     * @param date first day
     * @return every season
     */
    public List<LolResponses.AdminSeason> saveSeason(String key, LocalDate date) {
        requireAdmin();
        seasons.save(key, date);

        return adminSeasons();
    }

    /**
     * Deletes a season; its games stay and go to the season that covers their date.
     *
     * @param key short name
     * @return every season
     */
    public List<LolResponses.AdminSeason> deleteSeason(String key) {
        requireAdmin();
        seasons.delete(key);

        return adminSeasons();
    }

    /**
     * Updates a player if it is time to, waiting a moment for it. When there is a lot to bring it goes on in the
     * background and the answer says so.
     *
     * @param userId player
     * @param season short name of the season to answer with, or null for the current one
     * @return what is stored after it
     */
    public LolResponses.PlayerDetail refresh(UUID userId, String season) {
        CompletableFuture<Void> update = worker.refresh(find(userId));

        try {
            update.get(REFRESH_WAIT_SECONDS, TimeUnit.SECONDS);
            return detailOf(find(userId), season, false, null);
        } catch (TimeoutException exception) {
            return detailOf(find(userId), season, true, null);
        } catch (ExecutionException exception) {
            return detailOf(find(userId), season, false, LolSyncWorker.reasonOf(exception.getCause()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return detailOf(find(userId), season, false, null);
        }
    }

    /**
     * Links the caller with a Riot account.
     *
     * @param riotId Riot ID as {@code Name#TAG}
     * @return the players
     */
    public LolResponses.Players linkOwn(String riotId) {
        link(caller(), riotId);

        return players();
    }

    /**
     * Removes the link of the caller and their stored games.
     *
     * @return the players
     */
    public LolResponses.Players unlinkOwn() {
        unlink(caller());

        return players();
    }

    /**
     * Every account with the tag, for the admin screen.
     *
     * @return accounts with how their download is going
     */
    public List<LolResponses.AdminAccount> adminAccounts() {
        requireAdmin();

        Map<UUID, LolAccount> accounts = new LinkedHashMap<>();
        accountRepository.findAll().forEach(account -> accounts.put(account.getUserId(), account));

        Map<UUID, String> names = new LinkedHashMap<>(userDirectory.usersWithTag(LolModule.TAG));
        userDirectory.usernames(accounts.keySet()).forEach(names::putIfAbsent);

        return names.entrySet().stream()
                .map(entry -> adminAccountOf(entry.getKey(), entry.getValue(), accounts.get(entry.getKey())))
                .sorted(Comparator.comparing(LolResponses.AdminAccount::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Links an account for a player.
     *
     * @param userId player
     * @param riotId Riot ID as {@code Name#TAG}
     * @return every account
     */
    public List<LolResponses.AdminAccount> adminLink(UUID userId, String riotId) {
        requireAdmin();

        if (!userDirectory.usernames(List.of(userId)).containsKey(userId)) {
            throw ApiErrors.notFound("Account");
        }

        link(userId, riotId);

        return adminAccounts();
    }

    /**
     * Removes the link of a player and their stored games.
     *
     * @param userId player
     * @return every account
     */
    public List<LolResponses.AdminAccount> adminUnlink(UUID userId) {
        requireAdmin();
        unlink(userId);

        return adminAccounts();
    }

    /**
     * Starts the download of the games of a player.
     *
     * @param userId player
     * @return every account
     */
    public List<LolResponses.AdminAccount> download(UUID userId) {
        requireAdmin();
        worker.download(find(userId).getUserId());

        return adminAccounts();
    }

    /**
     * Starts the download of everybody who is missing it, one after another.
     *
     * @return every account
     */
    public List<LolResponses.AdminAccount> downloadAll() {
        requireAdmin();
        accountRepository.findAll().stream()
                .filter(account -> !LolAccount.COMPLETE.equals(account.getState()))
                .forEach(account -> worker.download(account.getUserId()));

        return adminAccounts();
    }

    /**
     * Stops the download of a player; what is stored stays.
     *
     * @param userId player
     * @return every account
     */
    public List<LolResponses.AdminAccount> pause(UUID userId) {
        requireAdmin();
        worker.pause(userId);

        return adminAccounts();
    }

    private void link(UUID userId, String riotId) {
        Matcher matcher = RIOT_ID.matcher(riotId == null ? "" : riotId);

        if (!matcher.matches()) {
            throw ApiErrors.badRequest("Escribe tu Riot ID como Nombre#TAG");
        }

        RiotModels.Account found;

        try {
            found = riotClient.account(matcher.group(1).trim(), matcher.group(2))
                    .orElseThrow(() -> ApiErrors.badRequest("No existe ninguna cuenta con ese Riot ID"));
        } catch (RiotApiException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, exception.getMessage());
        }

        accountRepository.findByPuuid(found.puuid())
                .filter(other -> !other.getUserId().equals(userId))
                .ifPresent(other -> {
                    throw ApiErrors.conflict("Esa cuenta ya está vinculada a otro jugador");
                });

        LolAccount account = accountRepository.findById(userId).orElse(null);

        if (account != null && !account.getPuuid().equals(found.puuid())) {
            unlink(userId);
            account = null;
        }

        if (account == null) {
            account = new LolAccount();
            account.setUserId(userId);
            account.setState(LolAccount.PENDING);
            account.setRanks(objectMapper.createArrayNode());
        }

        account.setPuuid(found.puuid());
        account.setGameName(found.gameName());
        account.setTagLine(found.tagLine());
        accountRepository.save(account);
    }

    private void unlink(UUID userId) {
        worker.pause(userId);
        accountRepository.findById(userId).ifPresent(accountRepository::delete);
    }

    private LolResponses.AdminAccount adminAccountOf(UUID userId, String username, LolAccount account) {
        if (account == null) {
            return new LolResponses.AdminAccount(userId, username, null, NOT_LINKED, 0, null, null, null, null);
        }

        LolSyncWorker.Progress progress = worker.progressOf(userId).orElse(null);

        return new LolResponses.AdminAccount(userId, username, riotIdOf(account), stateOf(account),
                matchRepository.countByUserId(userId), progress == null ? null : progress.saved(),
                progress == null ? null : progress.total(),
                progress == null ? null : (progress.total() - progress.saved()) * SECONDS_PER_GAME,
                account.getLastError());
    }

    private LolResponses.PlayerDetail detailOf(LolAccount account, String seasonKey, boolean updating, String error) {
        UUID userId = account.getUserId();
        String username = userDirectory.usernames(List.of(userId)).get(userId);
        LolSeasons.Season current = seasons.current();
        LolSeasons.Season season = seasonKey == null ? current : seasons.find(seasonKey).orElse(current);
        boolean isCurrent = season.key().equals(current.key());
        List<LolMatchRepository.ChampionTotals> totals =
                matchRepository.championTotals(userId, season.start(), season.end());
        List<LolResponses.QueueBlock> queues = LolQueue.ALL.stream()
                .map(queue -> queueBlock(userId, queue, season, totals))
                .toList();
        List<LolResponses.SeasonOption> options = seasons.all().reversed().stream()
                .filter(option -> option.key().equals(current.key()) || option.key().equals(season.key())
                        || matchRepository.existsByUserIdAndPlayedAtGreaterThanEqualAndPlayedAtLessThan(userId,
                        option.start(), option.end()))
                .map(option -> new LolResponses.SeasonOption(option.key(), option.label()))
                .toList();
        JsonNode ranks = isCurrent ? account.getRanks() : seasonRanks.find(userId, season.key());

        return new LolResponses.PlayerDetail(summaryOf(account, username), account.getCheckedAt(), updating, error,
                new LolResponses.SeasonOption(season.key(), season.label()), isCurrent, options, ranksOf(ranks), queues);
    }

    private LolResponses.QueueBlock queueBlock(UUID userId, LolQueue queue, LolSeasons.Season season,
                                               List<LolMatchRepository.ChampionTotals> totals) {
        List<LolMatchRepository.ChampionTotals> inQueue = totals.stream()
                .filter(row -> row.getQueueId() == queue.id())
                .toList();
        List<LolResponses.ChampionStats> champions = inQueue.stream()
                .sorted(Comparator.comparingLong(LolMatchRepository.ChampionTotals::getGames).reversed())
                .limit(TOP_CHAMPIONS)
                .map(row -> new LolResponses.ChampionStats(
                        dataDragon.championName(row.getChampionId(), row.getChampionName()),
                        dataDragon.championIcon(row.getChampionId()), row.getGames(),
                        percent(row.getWins(), row.getGames()),
                        kda(row.getKills(), row.getDeaths(), row.getAssists())))
                .toList();
        List<LolResponses.Match> matches = matchRepository
                .findTop20ByUserIdAndQueueIdAndPlayedAtGreaterThanEqualAndPlayedAtLessThanOrderByPlayedAtDesc(userId,
                        queue.id(), season.start(), season.end()).stream()
                .map(this::matchOf)
                .toList();

        return new LolResponses.QueueBlock(queue.id(), queue.label(), queue.emptyNotice(), statsOf(inQueue), champions,
                matches);
    }

    private static LolResponses.Stats statsOf(List<LolMatchRepository.ChampionTotals> rows) {
        long games = rows.stream().mapToLong(LolMatchRepository.ChampionTotals::getGames).sum();
        long wins = rows.stream().mapToLong(LolMatchRepository.ChampionTotals::getWins).sum();
        long kills = rows.stream().mapToLong(LolMatchRepository.ChampionTotals::getKills).sum();
        long deaths = rows.stream().mapToLong(LolMatchRepository.ChampionTotals::getDeaths).sum();
        long assists = rows.stream().mapToLong(LolMatchRepository.ChampionTotals::getAssists).sum();
        long cs = rows.stream().mapToLong(LolMatchRepository.ChampionTotals::getCs).sum();
        long seconds = rows.stream().mapToLong(LolMatchRepository.ChampionTotals::getSeconds).sum();

        return new LolResponses.Stats(games, wins, percent(wins, games), kda(kills, deaths, assists),
                average(kills, games), average(deaths, games), average(assists, games),
                seconds == 0 ? 0 : round(cs * 60.0 / seconds));
    }

    private LolResponses.Match matchOf(LolMatch match) {
        List<String> items = Arrays.stream(match.getItems().split(","))
                .map(item -> dataDragon.itemIcon(Integer.parseInt(item)))
                .filter(Objects::nonNull)
                .toList();
        List<String> spells = new ArrayList<>();

        for (int spell : new int[]{match.getSpell1(), match.getSpell2()}) {
            String icon = dataDragon.spellIcon(spell);

            if (icon != null) {
                spells.add(icon);
            }
        }

        return new LolResponses.Match(match.getMatchId(), match.getPlayedAt(), match.getDurationSeconds(),
                dataDragon.championName(match.getChampionId(), match.getChampionName()),
                dataDragon.championIcon(match.getChampionId()), match.isWin(), match.isRemake(), match.getKills(),
                match.getDeaths(), match.getAssists(), match.getCs(), match.getDamage(), match.getGold(),
                match.getVision(), match.getChampionLevel(), items, spells);
    }

    /**
     * Official ranks as they are stored, SoloQ first.
     *
     * @param stored the ranks kept
     * @return ranks
     */
    static List<LolResponses.Rank> ranksOf(JsonNode stored) {
        List<LolResponses.Rank> ranks = new ArrayList<>();

        for (String queueType : List.of("RANKED_SOLO_5x5", "RANKED_FLEX_SR")) {
            for (JsonNode rank : stored) {
                if (queueType.equals(rank.path("queueType").asText())) {
                    ranks.add(new LolResponses.Rank(RANKED.get(queueType),
                            tierOf(rank.path("tier").asText(), rank.path("rank").asText()),
                            rank.path("leaguePoints").asInt(), rank.path("wins").asInt(), rank.path("losses").asInt()));
                }
            }
        }

        return ranks;
    }

    private static String tierOf(String tier, String division) {
        String name = TIERS.getOrDefault(tier, tier.toLowerCase(Locale.ROOT));

        return Arrays.asList(APEX).contains(tier) ? name : name + " " + division;
    }

    private LolResponses.PlayerSummary summaryOf(LolAccount account, String username) {
        return new LolResponses.PlayerSummary(account.getUserId(), username, riotIdOf(account), stateOf(account),
                dataDragon.profileIcon(account.getProfileIconId()), account.getSummonerLevel());
    }

    private String stateOf(LolAccount account) {
        return worker.phaseOf(account.getUserId())
                .orElse(LolAccount.LOADING.equals(account.getState()) ? INTERRUPTED : account.getState());
    }

    private static String riotIdOf(LolAccount account) {
        return account.getGameName() + "#" + account.getTagLine();
    }

    private LolAccount find(UUID userId) {
        return accountRepository.findById(userId).orElseThrow(() -> ApiErrors.notFound("Account"));
    }

    private UUID caller() {
        return currentUser.id().orElseThrow(ApiErrors::forbidden);
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw ApiErrors.forbidden();
        }
    }

    private static int percent(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }

    private static double kda(long kills, long deaths, long assists) {
        return round((kills + assists) / (double) Math.max(1, deaths));
    }

    private static double average(long value, long games) {
        return games == 0 ? 0 : round(value / (double) games);
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
