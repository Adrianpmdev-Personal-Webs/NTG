package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.export.GameDataExporter;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.GameDataStore;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.WagoClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Keeps the game data up to date with the game: it asks wago.tools for the newest WoW Forever build every
 * {@code checkMinutes} of config.json and, when there is a new one, builds the data file from that build's client
 * tables (compared with the newest Classic Era build), checks it, serves it and keeps a copy in Storage.
 * <p>
 * When the backend starts it first serves the copy kept in Storage, so players do not wait for a build. While no data
 * is served (no copy and wago.tools unreachable) it tries again every few minutes. A failed check, a broken table or
 * an incomplete file never replaces the data being served.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
public class GameDataSyncService {

    /** How soon to try again while no data is served. */
    private static final Duration RETRY = Duration.ofMinutes(5);

    private final GameDataService gameDataService;
    private final WagoClient wagoClient;
    private final GameDataStore store;
    private final TaskScheduler scheduler;
    private final Clock clock;

    /**
     * Builds the service on the system clock.
     *
     * @param gameDataService serves the data
     * @param wagoClient      reads the builds and tables of the game clients
     * @param store           keeps the last good data file
     * @param scheduler       runs the checks
     */
    @Autowired
    public GameDataSyncService(GameDataService gameDataService, WagoClient wagoClient, GameDataStore store,
                               TaskScheduler scheduler) {
        this(gameDataService, wagoClient, store, scheduler, Clock.systemUTC());
    }

    GameDataSyncService(GameDataService gameDataService, WagoClient wagoClient, GameDataStore store,
                        TaskScheduler scheduler, Clock clock) {
        this.gameDataService = gameDataService;
        this.wagoClient = wagoClient;
        this.store = store;
        this.scheduler = scheduler;
        this.clock = clock;
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

        if (newest.isEmpty() || newest.equals(gameDataService.currentBuild())) {
            return;
        }

        log.info("Reading WoW Forever build {} from the game client", newest.get());
        long start = clock.millis();
        ObjectNode data = new GameDataExporter(gameDataService.getConfig()).export(
                new GameDataExporter.Build(newest.get(), wagoClient.tablesOf(newest.get())),
                classicBuild(settings.classicProduct()), LocalDate.now(clock));
        gameDataService.publish(data);
        log.info("WoW Forever build {} is served (read in {} ms)", newest.get(), clock.millis() - start);
        keep(data);
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
        try {
            store.save(data);
        } catch (RuntimeException exception) {
            log.warn("The WoW Forever game data is served but could not be kept: {}", exception.getMessage());
        }
    }
}
