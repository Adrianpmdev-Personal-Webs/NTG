package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.platform.status.ModuleStatus;
import com.adrianpmdev.platform.status.ModuleStatus.Level;
import com.adrianpmdev.platform.status.ModuleStatus.Line;
import com.adrianpmdev.platform.status.ModuleStatusProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tells the admin dashboard how the game data is doing: the build being served, when it was generated, the newest
 * build wago.tools has and how the last check went. Everything comes from memory, so it costs no call.
 *
 * @author adrianpmdev
 */
@Component
public class GameDataStatus implements ModuleStatusProvider {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Madrid"));
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String NONE = "—";

    private final GameDataService gameDataService;
    private final GameDataSyncService syncService;

    /**
     * Builds the provider.
     *
     * @param gameDataService the data being served
     * @param syncService     how the checks are going
     */
    public GameDataStatus(GameDataService gameDataService, GameDataSyncService syncService) {
        this.gameDataService = gameDataService;
        this.syncService = syncService;
    }

    @Override
    public ModuleStatus moduleStatus() {
        Optional<String> served = gameDataService.currentBuild();
        GameDataSyncService.Progress progress = syncService.progress();
        boolean behind = progress.newestBuild() != null && !served.map(progress.newestBuild()::equals).orElse(false);

        return new ModuleStatus("WoW Forever · Datos del juego", List.of(
                new Line("Build servida", served.orElse("Ninguna"), served.isPresent() ? Level.OK : Level.ERROR, "package"),
                new Line("Generada el", gameDataService.currentGenerated().map(GameDataStatus::day).orElse(NONE), Level.OK,
                        "calendar"),
                new Line("Build más nueva en wago", progress.newestBuild() == null ? NONE : progress.newestBuild(),
                        behind ? Level.WARNING : Level.OK, "download"),
                new Line("Última comprobación", date(progress.lastCheckAt()),
                        progress.lastCheckAt() == null ? Level.WARNING : Level.OK, "clock"),
                new Line("Último error", progress.lastError() == null ? "Ninguno"
                        : date(progress.lastErrorAt()) + " · " + progress.lastError(),
                        progress.failing() ? Level.ERROR : Level.OK, "alert")));
    }

    private static String day(String isoDate) {
        return DAY.format(LocalDate.parse(isoDate));
    }

    private static String date(Instant instant) {
        return instant == null ? NONE : DATE.format(instant);
    }
}
