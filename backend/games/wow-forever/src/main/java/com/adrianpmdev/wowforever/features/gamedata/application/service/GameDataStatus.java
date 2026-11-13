package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.platform.status.ModuleStatus;
import com.adrianpmdev.platform.status.ModuleStatus.Level;
import com.adrianpmdev.platform.status.ModuleStatus.Line;
import com.adrianpmdev.platform.status.ModuleStatusProvider;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.PendingBuild;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tells the admin dashboard how the game data is doing: the build being served, when it was generated, the newest
 * build wago.tools has, the build waiting to be read, the Classic Era build it is compared with and how the last check
 * went. Everything comes from memory, so it costs no call.
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
                pendingLine(progress.pending()),
                new Line("Referencia de Classic Era", gameDataService.currentClassicBuild().orElse(NONE), Level.OK,
                        "storage"),
                new Line("Último error", progress.lastError() == null ? "Ninguno"
                        : date(progress.lastErrorAt()) + " · " + progress.lastError(),
                        progress.failing() ? Level.ERROR : Level.OK, "alert")));
    }

    /** The build waiting to be read: a warning while it is tried, an error once it is left alone. */
    private static Line pendingLine(PendingBuild pending) {
        if (pending == null) {
            return new Line("Build pendiente", "Ninguna", Level.OK, "refresh");
        }

        boolean left = pending.attempts() >= GameDataSyncService.MAX_ATTEMPTS;
        String state = left ? "abandonada tras " + pending.attempts() + " intentos"
                : "intento " + pending.attempts() + " de " + GameDataSyncService.MAX_ATTEMPTS;
        String error = pending.lastError() == null ? (pending.attempts() > 0 ? " · tumbó el backend" : "")
                : " · " + pending.lastError();

        return new Line("Build pendiente", pending.build() + " · " + state + error, left ? Level.ERROR : Level.WARNING,
                "refresh");
    }

    private static String day(String isoDate) {
        return DAY.format(LocalDate.parse(isoDate));
    }

    private static String date(Instant instant) {
        return instant == null ? NONE : DATE.format(instant);
    }
}
