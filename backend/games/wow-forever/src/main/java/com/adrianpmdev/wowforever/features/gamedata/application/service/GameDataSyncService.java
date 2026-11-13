package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.export.GameDataExporter;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.GameDataStore;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.PendingBuild;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.WagoClient;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.WagoUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.function.Function;

/**
 * Keeps the game data up to date with the game. Every {@code checkMinutes} of config.json it asks wago.tools for the
 * newest WoW Forever build; when it is not the one being served (or the file served was written by another version of
 * the export, see GameDataExporter.fingerprint), that build is read from its client tables, compared with the Classic
 * reference, checked, served and kept in Storage.
 * <p>
 * Reading a build is what needs the most memory, and a backend that has been running for a while has too little left
 * for it on the free server. So a build is only read on a clean start:
 * <ol>
 *     <li>a check that finds a new build notes it in Storage as pending and restarts the backend;</li>
 *     <li>the new backend serves the kept copy and, in its first check, reads the pending build;</li>
 *     <li>once the build is served and kept, the note is removed and the backend restarts again, to give back the
 *     memory the read leaves behind (classes and compiled code that stay until the process ends).</li>
 * </ol>
 * A check that finds the new build within the first minutes of a backend that has read nothing yet (after a deploy,
 * say) reads it at once: it already is a clean start.
 * <p>
 * The note counts the reads of the build that were started and never ended well. A read that stops the backend (it
 * runs out of memory) is found in the note by the next start, which tries once more; after {@value #MAX_ATTEMPTS}
 * attempts the build is left alone until a newer one comes out or a new deploy, and the admin dashboard shows it. A
 * read that fails because wago.tools was unavailable does not count as an attempt. A build is never read without its
 * note, since a read that stops the backend would then be repeated on every start.
 * <p>
 * Only the deployed backend keeps copies and notes in Storage. A backend run on a developer machine
 * ({@code wow-forever.sync.keep-copy=false}) reads the kept copy and the Classic reference, reads a new build in place
 * and never writes to Storage or restarts, so trying unfinished code can never leave a wrong copy for the deployed one.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
public class GameDataSyncService {

    /** Attempts at reading one build before it is left alone. */
    public static final int MAX_ATTEMPTS = 2;

    /** How soon to try again while nothing is served, or while a clean start waits to read a build. */
    private static final Duration RETRY = Duration.ofMinutes(5);

    /** How long after its start a backend that has read nothing still counts as a clean start. */
    private static final Duration CLEAN_START = Duration.ofMinutes(15);

    private static final long MILLIS_PER_SECOND = 1000;

    private final GameDataService gameDataService;
    private final WagoClient wagoClient;
    private final GameDataStore store;
    private final TaskScheduler scheduler;
    private final BackendRestart backendRestart;
    private final Clock clock;
    private final boolean keepCopy;
    private final String deploy;
    private final Function<GameDataConfig, GameDataExporter> exporters;
    private final Instant startedAt;

    /** Fingerprint of this version of the export; null until first needed. */
    private String exporter;

    /** The note of the pending build: null until read from Storage, then kept in step with every change to it. */
    private Optional<PendingBuild> pending;

    /** Whether this backend has read a build, so it no longer is a clean start. */
    private boolean readDone;

    /** A file being served whose copy could not be kept, or null: it is kept on the next check. */
    private ObjectNode unkept;

    /** Whether the backend is restarting: no further check is planned. */
    private volatile boolean restarting;

    /** How the checks are going since this backend started. */
    private volatile Progress progress = new Progress(null, null, null, null, false, null);

    /**
     * How the checks are going since this backend started, for the admin dashboard.
     *
     * @param lastCheckAt when wago.tools was last asked, or null
     * @param newestBuild newest build wago.tools answered, or null
     * @param lastError   what went wrong the last time a check failed, or null
     * @param lastErrorAt when that happened, or null
     * @param failing     whether the last check failed
     * @param pending     the build waiting to be read and its attempts, or null
     */
    public record Progress(Instant lastCheckAt, String newestBuild, String lastError, Instant lastErrorAt,
                           boolean failing, PendingBuild pending) {

        private Progress checked(Instant at) {
            return new Progress(at, newestBuild, lastError, lastErrorAt, false, pending);
        }

        private Progress failed(Instant at, String error) {
            return new Progress(at, newestBuild, error, at, true, pending);
        }

        private Progress newest(String build) {
            return new Progress(lastCheckAt, build, lastError, lastErrorAt, failing, pending);
        }

        private Progress pending(PendingBuild note) {
            return new Progress(lastCheckAt, newestBuild, lastError, lastErrorAt, failing, note);
        }
    }

    /**
     * Builds the service on the system clock.
     *
     * @param gameDataService serves the data
     * @param wagoClient      reads the builds and tables of the game clients
     * @param store           keeps the data file, the Classic reference and the pending build
     * @param scheduler       runs the checks
     * @param backendRestart  restarts the backend
     * @param keepCopy        whether this backend writes to Storage and restarts (false on a developer machine)
     * @param deploy          the commit Render deploys (empty elsewhere): a new deploy tries a left build again
     */
    @Autowired
    public GameDataSyncService(GameDataService gameDataService, WagoClient wagoClient, GameDataStore store,
                               TaskScheduler scheduler, BackendRestart backendRestart,
                               @Value("${wow-forever.sync.keep-copy:true}") boolean keepCopy,
                               @Value("${RENDER_GIT_COMMIT:}") String deploy) {
        this(gameDataService, wagoClient, store, scheduler, backendRestart, Clock.systemUTC(), keepCopy, deploy,
                GameDataExporter::new);
    }

    GameDataSyncService(GameDataService gameDataService, WagoClient wagoClient, GameDataStore store,
                        TaskScheduler scheduler, BackendRestart backendRestart, Clock clock, boolean keepCopy,
                        String deploy, Function<GameDataConfig, GameDataExporter> exporters) {
        this.gameDataService = gameDataService;
        this.wagoClient = wagoClient;
        this.store = store;
        this.scheduler = scheduler;
        this.backendRestart = backendRestart;
        this.clock = clock;
        this.keepCopy = keepCopy;
        this.deploy = deploy;
        this.exporters = exporters;
        this.startedAt = clock.instant();
    }

    /** Starts the checks once the application is up. */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        scheduler.schedule(this::checkAndReschedule, clock.instant());
    }

    /** How the checks are going since this backend started. */
    public Progress progress() {
        return progress;
    }

    private void checkAndReschedule() {
        String error = null;

        try {
            refresh();
            progress = progress.checked(clock.instant());
        } catch (IOException | RuntimeException exception) {
            error = String.valueOf(exception.getMessage());
            progress = progress.failed(clock.instant(), error);
        }

        if (restarting) {
            return;
        }

        Duration next = nextCheck(error != null);

        if (error != null) {
            log.warn("WoW game data: the check failed ({}); serving {}; next check in {} min", error, served(),
                    next.toMinutes());
        }

        scheduler.schedule(this::checkAndReschedule, clock.instant().plus(next));
    }

    private Duration nextCheck(boolean failed) {
        if (gameDataService.currentBuild().isEmpty() || (failed && keepCopy && cleanStart())) {
            return RETRY;
        }

        return Duration.ofMinutes(gameDataService.getConfig().client().checkMinutes());
    }

    /**
     * One check: serves the kept copy when nothing is served yet, then compares the newest build with the one served
     * and reads it, or restarts to read it, when they differ.
     *
     * @throws IOException when wago.tools cannot be reached or the read fails
     */
    synchronized void refresh() throws IOException {
        if (gameDataService.currentBuild().isEmpty()) {
            restore();
        }

        String product = gameDataService.getConfig().client().product();
        Optional<String> newest = wagoClient.latestBuild(product);
        newest.ifPresent(build -> progress = progress.newest(build));

        if (newest.isEmpty()) {
            log.warn("WoW game data: wago.tools lists no build of {}; serving {}", product, served());
            return;
        }

        String build = newest.get();

        if (gameDataService.currentBuild().filter(build::equals).isPresent()
                && exporter().equals(gameDataService.currentExporter())) {
            upToDate(build);
        } else {
            readOrRestart(build);
        }
    }

    /* ─── The newest build is served ─── */

    private void upToDate(String build) {
        if (keepCopy && unkept != null) {
            keep(unkept, build);
            return;
        }

        if (keepCopy) {
            pending().ifPresent(note -> {
                deletePending();
                log.info("WoW game data: build {} is served and kept; the note of pending build {} is removed",
                        build, note.build());
            });
        }

        log.info("WoW game data: checked wago.tools, the newest build {} is the one served, nothing to read", build);
    }

    /* ─── A build to read ─── */

    private void readOrRestart(String build) throws IOException {
        String found = gameDataService.currentBuild().filter(build::equals).isPresent()
                ? "build " + build + " is served but was written by another version of the export"
                : "new build " + build + " found (serving " + served() + ")";

        if (!keepCopy) {
            log.info("WoW game data: {}, reading it here (developer machine: nothing is kept and the backend does not "
                    + "restart)", found);
            read(build);
            return;
        }

        PendingBuild note = pending()
                .filter(kept -> kept.build().equals(build) && kept.exporter().equals(exporter())
                        && kept.deploy().equals(deploy))
                .orElse(new PendingBuild(build, exporter(), deploy, 0, null));

        if (note.attempts() >= MAX_ATTEMPTS) {
            progress = progress.pending(note);
            log.error("WoW game data: build {} failed {} times (last: {}); it is not read again until a newer build "
                            + "comes out or a new deploy; serving {}", build, note.attempts(), lastFailure(note),
                    served());
            return;
        }

        if (!cleanStart()) {
            restartToRead(found, note);
            return;
        }

        if (note.attempts() > 0) {
            log.warn("WoW game data: attempt {} of {} at build {} {}", note.attempts(), MAX_ATTEMPTS, build,
                    lastFailure(note));
        }

        PendingBuild attempt = new PendingBuild(build, exporter(), deploy, note.attempts() + 1, null);

        if (!savePending(attempt, found + ", but its read could not be noted in Storage; it is not read without the "
                + "note, as a read that stops the backend would be repeated on every start")) {
            return;
        }

        log.info("WoW game data: {}, reading it now on a clean start (attempt {} of {})", found, attempt.attempts(),
                MAX_ATTEMPTS);
        ObjectNode data;

        try {
            data = read(build);
        } catch (IOException | RuntimeException exception) {
            failed(note, attempt, exception);
            throw exception;
        }

        keep(data, build);
    }

    /** Notes the build as pending and restarts, so that the next start reads it with all its memory. */
    private void restartToRead(String found, PendingBuild note) {
        if (savePending(note, found + ", but it could not be noted in Storage, so the backend does not restart to "
                + "read it")) {
            log.info("WoW game data: {}, restarting to read it on a clean start", found);
            restart();
        }
    }

    /** Notes why the read failed; a failure of wago.tools does not count as an attempt. */
    private void failed(PendingBuild before, PendingBuild attempt, Exception exception) {
        boolean passing = unavailable(exception);
        PendingBuild after = new PendingBuild(attempt.build(), attempt.exporter(), attempt.deploy(),
                passing ? before.attempts() : attempt.attempts(), String.valueOf(exception.getMessage()));
        savePending(after, "the failed read of build " + attempt.build() + " could not be noted in Storage");
        log.error("WoW game data: reading build {} failed{}: {}", attempt.build(), passing
                        ? " because wago.tools was unavailable (it does not count as an attempt)"
                        : " (attempt " + attempt.attempts() + " of " + MAX_ATTEMPTS + ")",
                exception.getMessage());
    }

    /** Reads a build, compared with the Classic reference, and serves it. */
    private ObjectNode read(String build) throws IOException {
        long start = clock.millis();

        try {
            GameDataExporter export = exporters.apply(gameDataService.getConfig());
            JsonNode classic = classicReference(export);
            ObjectNode data = export.export(new GameDataExporter.Build(build, wagoClient.tablesOf(build)), classic,
                    LocalDate.now(clock));
            gameDataService.publish(data);
            log.info("WoW game data: build {} read and served in {} s{}", build,
                    (clock.millis() - start) / MILLIS_PER_SECOND, ProcessMemory.peakMegabytes()
                            .map(peak -> ", process peak " + peak + " MB").orElse(""));

            return data;
        } finally {
            readDone = true;
            System.gc();
        }
    }

    /** The kept Classic reference, or one read now from the newest Classic Era build (and kept). */
    private JsonNode classicReference(GameDataExporter export) throws IOException {
        Optional<JsonNode> kept = Optional.empty();

        try {
            kept = store.loadClassic();
        } catch (RuntimeException exception) {
            log.warn("WoW game data: the Classic Era reference kept in Storage could not be read ({})",
                    exception.getMessage());
        }

        if (kept.isPresent() && GameDataExporter.fits(kept.get(), exporter())) {
            log.info("WoW game data: Classic Era reference {} loaded from Storage", kept.get().path("build").asText());
            return kept.get();
        }

        String product = gameDataService.getConfig().client().classicProduct();
        String version = wagoClient.latestBuild(product)
                .orElseThrow(() -> new IOException("wago.tools lists no build of " + product));
        log.info("WoW game data: reading Classic Era build {} to keep it as the reference{}", version,
                kept.isPresent() ? " (the kept one was written by another version of the export)" : "");
        ObjectNode reference = export.classicReference(new GameDataExporter.Build(version,
                wagoClient.tablesOf(version)));

        if (keepCopy) {
            try {
                store.saveClassic(reference);
                log.info("WoW game data: Classic Era reference {} is kept in Storage", version);
            } catch (RuntimeException exception) {
                log.warn("WoW game data: the Classic Era reference could not be kept in Storage ({}); it is read again "
                        + "with the next build", exception.getMessage());
            }
        }

        return reference;
    }

    /** Keeps a file just served, removes the note of its build and restarts; or waits for the next check. */
    private void keep(ObjectNode data, String build) {
        try {
            store.save(data);
        } catch (RuntimeException exception) {
            unkept = data;
            log.warn("WoW game data: build {} is served but its copy could not be kept in Storage ({}); the backend "
                    + "does not restart until it is kept, the next check tries again", build, exception.getMessage());
            return;
        }

        unkept = null;
        deletePending();
        log.info("WoW game data: build {} is kept in Storage, restarting to free the memory of the read", build);
        restart();
    }

    /* ─── Kept copy and note ─── */

    /** Serves the copy kept in Storage, if there is a good one. */
    private void restore() {
        try {
            Optional<JsonNode> kept = store.load();

            if (kept.isEmpty()) {
                log.info("WoW game data: no copy kept in Storage yet");
                return;
            }

            gameDataService.publish(kept.get());
            log.info("WoW game data: serving {} from the copy kept in Storage (generated {}{})", served(),
                    gameDataService.currentGenerated().orElse("?"),
                    exporter().equals(gameDataService.currentExporter()) ? ""
                            : ", by another version of the export: it is read again");
        } catch (RuntimeException exception) {
            log.warn("WoW game data: the copy kept in Storage could not be used ({})", exception.getMessage());
        }
    }

    private Optional<PendingBuild> pending() {
        if (pending == null) {
            try {
                pending = store.pending();
                progress = progress.pending(pending.orElse(null));
            } catch (RuntimeException exception) {
                log.warn("WoW game data: the note of the pending build could not be read ({})", exception.getMessage());
                return Optional.empty();
            }
        }

        return pending;
    }

    /** Writes the note, or logs why it could not be written. */
    private boolean savePending(PendingBuild note, String whenItFails) {
        try {
            store.savePending(note);
        } catch (RuntimeException exception) {
            log.warn("WoW game data: {} ({})", whenItFails, exception.getMessage());
            return false;
        }

        pending = Optional.of(note);
        progress = progress.pending(note);

        return true;
    }

    private void deletePending() {
        try {
            store.deletePending();
        } catch (RuntimeException exception) {
            log.warn("WoW game data: the note of the pending build could not be removed ({}); the next check removes "
                    + "it", exception.getMessage());
            return;
        }

        pending = Optional.empty();
        progress = progress.pending(null);
    }

    /* ─── Helpers ─── */

    private String exporter() {
        if (exporter == null) {
            exporter = GameDataExporter.fingerprint(gameDataService.getConfig());
            log.info("WoW game data: export fingerprint {}", exporter);
        }

        return exporter;
    }

    /** Whether this backend started a short while ago and has read nothing yet. */
    private boolean cleanStart() {
        return !readDone && clock.instant().isBefore(startedAt.plus(CLEAN_START));
    }

    private void restart() {
        restarting = true;
        backendRestart.run();
    }

    private String served() {
        return gameDataService.currentBuild().map(build -> "build " + build).orElse("nothing");
    }

    private static String lastFailure(PendingBuild note) {
        return note.lastError() == null ? "stopped the backend" : "failed: " + note.lastError();
    }

    private static boolean unavailable(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof WagoUnavailableException) {
                return true;
            }
        }

        return false;
    }
}
