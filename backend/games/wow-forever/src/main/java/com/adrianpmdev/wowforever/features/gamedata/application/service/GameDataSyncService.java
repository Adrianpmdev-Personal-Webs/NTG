package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.export.GameDataExporter;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.GameDataStore;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.WagoClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Keeps the game data up to date with the game: it asks wago.tools for the newest WoW Forever build every
 * {@code checkMinutes} of config.json and, when there is a new one, builds the data file from that build's client
 * tables (compared with the newest Classic Era build), checks it, serves it and keeps a copy in Storage. The build
 * being served is also read again, once, when its file was written by another version of the export (its fingerprint
 * does not match, see GameDataExporter.fingerprint): any change to the export shows up on the next start, while the
 * old file keeps being served.
 * <p>
 * Only the deployed backend keeps copies and marks in Storage. A backend run on a developer machine
 * ({@code wow-forever.sync.keep-copy=false}) reads the kept copy and may read a build again in its own memory, but
 * never writes to Storage, so trying unfinished code can never leave a wrong copy for the deployed one.
 * <p>
 * When the backend starts it first serves the copy kept in Storage, so players do not wait for a build. While no data
 * is served (no copy and wago.tools unreachable) it tries again every few minutes. A failed check, a broken table or
 * an incomplete file never replaces the data being served.
 * <p>
 * Reading a build is what needs the most memory. If a read ever stops the backend (out of memory), the restart would
 * try the same build again and fall into a loop, so every read leaves a mark in Storage that is removed when it ends:
 * a mark found after a restart means that build stopped the backend, and it is not read again for
 * {@value #HOURS_AFTER_A_CRASH} hours (the kept copy keeps being served).
 * <p>
 * A read leaves well over a hundred megabytes of garbage behind, and the serial collector only gives memory back to
 * the system after a full collection, which a quiet backend may not run for days. So every read ends with one (a
 * pause well under a second, once per new build): with the heap settings of the Dockerfile the backend returns to its
 * usual memory instead of keeping the peak reserved.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
public class GameDataSyncService {

    /** How soon to try again while no data is served. */
    private static final Duration RETRY = Duration.ofMinutes(5);

    /** How long a build whose read stopped the backend waits before it is read again. */
    private static final int HOURS_AFTER_A_CRASH = 24;

    private final GameDataService gameDataService;
    private final WagoClient wagoClient;
    private final GameDataStore store;
    private final TaskScheduler scheduler;
    private final Clock clock;
    private final boolean keepCopy;

    /** Fingerprint of this version of the export; null until first needed. */
    private String exporter;

    /** The read a previous run of the backend never ended; null until first asked, then fixed for this run. */
    private Optional<GameDataStore.Attempt> crashedRead;

    /**
     * Builds the service on the system clock.
     *
     * @param gameDataService serves the data
     * @param wagoClient      reads the builds and tables of the game clients
     * @param store           keeps the last good data file
     * @param scheduler       runs the checks
     * @param keepCopy        whether this backend writes the copy and the marks in Storage (false on a developer
     *                        machine)
     */
    @Autowired
    public GameDataSyncService(GameDataService gameDataService, WagoClient wagoClient, GameDataStore store,
                               TaskScheduler scheduler,
                               @Value("${wow-forever.sync.keep-copy:true}") boolean keepCopy) {
        this(gameDataService, wagoClient, store, scheduler, Clock.systemUTC(), keepCopy);
    }

    GameDataSyncService(GameDataService gameDataService, WagoClient wagoClient, GameDataStore store,
                        TaskScheduler scheduler, Clock clock, boolean keepCopy) {
        this.gameDataService = gameDataService;
        this.wagoClient = wagoClient;
        this.store = store;
        this.scheduler = scheduler;
        this.clock = clock;
        this.keepCopy = keepCopy;
    }

    /** Starts the checks once the application is up. */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        scheduler.schedule(this::checkAndReschedule, clock.instant());
    }

    private void checkAndReschedule() {
        try {
            refresh();
        } catch (IOException | RuntimeException exception) {
            log.warn("WoW Forever game data could not be updated: {}", exception.getMessage());
        } finally {
            Duration next = gameDataService.currentBuild().isPresent()
                    ? Duration.ofMinutes(gameDataService.getConfig().client().checkMinutes())
                    : RETRY;
            scheduler.schedule(this::checkAndReschedule, clock.instant().plus(next));
        }
    }

    /**
     * One check: serves the kept copy when nothing is served yet, then builds and serves the newest build when it is
     * not the one being served.
     *
     * @throws IOException when wago.tools cannot be reached
     */
    synchronized void refresh() throws IOException {
        if (gameDataService.currentBuild().isEmpty()) {
            restore();
        }

        GameDataConfig.Client settings = gameDataService.getConfig().client();
        Optional<String> newest = wagoClient.latestBuild(settings.product());

        boolean upToDate = newest.equals(gameDataService.currentBuild())
                && exporter().equals(gameDataService.currentExporter());

        if (newest.isEmpty() || upToDate || stoppedTheBackend(newest.get())) {
            return;
        }

        log.info("Reading WoW Forever build {} from the game client{}", newest.get(),
                newest.equals(gameDataService.currentBuild()) ? " again: it was kept by another version of the export"
                        : "");
        long start = clock.millis();
        markRead(new GameDataStore.Attempt(newest.get(), clock.instant()));

        try {
            ObjectNode data = new GameDataExporter(gameDataService.getConfig()).export(
                    new GameDataExporter.Build(newest.get(), wagoClient.tablesOf(newest.get())),
                    classicBuild(settings.classicProduct()), LocalDate.now(clock));
            gameDataService.publish(data);
            log.info("WoW Forever build {} is served (read in {} ms)", newest.get(), clock.millis() - start);
            keep(data);
        } finally {
            unmarkRead();
            System.gc();
        }
    }

    private String exporter() {
        if (exporter == null) {
            exporter = GameDataExporter.fingerprint(gameDataService.getConfig());
            log.info("WoW Forever export fingerprint {}", exporter);
        }

        return exporter;
    }

    /** Whether a previous run of the backend stopped while reading this build, less than a day ago. */
    private boolean stoppedTheBackend(String build) {
        if (crashedRead == null) {
            try {
                crashedRead = store.unfinishedRead();
            } catch (RuntimeException exception) {
                log.warn("The mark of an unfinished read could not be checked: {}", exception.getMessage());
                crashedRead = Optional.empty();
            }
        }

        if (crashedRead.isEmpty() || !crashedRead.get().build().equals(build)) {
            return false;
        }

        Instant retryAt = crashedRead.get().startedAt().plus(Duration.ofHours(HOURS_AFTER_A_CRASH));

        if (!clock.instant().isBefore(retryAt)) {
            return false;
        }

        log.error("WoW Forever build {} stopped the backend while it was being read at {}: it is not read again "
                + "until {}", build, crashedRead.get().startedAt(), retryAt);

        return true;
    }

    private void markRead(GameDataStore.Attempt attempt) {
        if (!keepCopy) {
            return;
        }

        try {
            store.startRead(attempt);
        } catch (RuntimeException exception) {
            log.warn("The read of build {} could not be marked: {}", attempt.build(), exception.getMessage());
        }
    }

    private void unmarkRead() {
        if (!keepCopy) {
            return;
        }

        try {
            store.endRead();
        } catch (RuntimeException exception) {
            log.warn("The mark of the read could not be removed: {}", exception.getMessage());
        }
    }

    /** Serves the copy kept in Storage, if there is a good one. */
    private void restore() {
        try {
            store.load().ifPresent(kept -> {
                gameDataService.publish(kept);
                log.info("WoW Forever build {} is served from the kept copy",
                        gameDataService.currentBuild().orElse("?"));
            });
        } catch (RuntimeException exception) {
            log.warn("The kept WoW Forever game data could not be used: {}", exception.getMessage());
        }
    }

    /** The newest Classic Era build to compare with, or null when it cannot be found: the data is then not compared. */
    private GameDataExporter.Build classicBuild(String product) {
        try {
            return wagoClient.latestBuild(product)
                    .map(version -> new GameDataExporter.Build(version, wagoClient.tablesOf(version)))
                    .orElse(null);
        } catch (IOException exception) {
            log.warn("No Classic Era build to compare with: {}", exception.getMessage());

            return null;
        }
    }

    private void keep(ObjectNode data) {
        if (!keepCopy) {
            log.info("The WoW Forever game data is not kept in Storage (wow-forever.sync.keep-copy=false)");
            return;
        }

        try {
            store.save(data);
        } catch (RuntimeException exception) {
            log.warn("The WoW Forever game data is served but could not be kept: {}", exception.getMessage());
        }
    }
}
