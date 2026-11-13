package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.platform.status.ModuleStatus;
import com.adrianpmdev.platform.status.ModuleStatus.Level;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.PendingBuild;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What the dashboard highlights about the game data.
 *
 * @author adrianpmdev
 */
class GameDataStatusTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private final GameDataService gameDataService = mock(GameDataService.class);
    private final GameDataSyncService syncService = mock(GameDataSyncService.class);
    private final GameDataStatus status = new GameDataStatus(gameDataService, syncService);

    @Test
    void aServedBuildThatIsTheNewestIsFine() {
        served("1.60.1.70707");
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", null, null, false,
                null));

        assertThat(status.moduleStatus().lines()).allSatisfy(line -> assertThat(line.level()).isEqualTo(Level.OK));
    }

    @Test
    void aNewerBuildInWagoIsAWarning() {
        served("1.60.1.70685");
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", null, null, false,
                null));

        assertThat(level("Build más nueva en wago")).isEqualTo(Level.WARNING);
    }

    @Test
    void aFailingCheckIsAnErrorWithItsMessage() {
        served("1.60.1.70707");
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", "wago down", NOW, true,
                null));

        assertThat(level("Último error")).isEqualTo(Level.ERROR);
        assertThat(line("Último error").value()).contains("wago down");
    }

    @Test
    void anErrorThatPassedStaysVisibleButIsNotHighlighted() {
        served("1.60.1.70707");
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", "wago down", NOW, false,
                null));

        assertThat(level("Último error")).isEqualTo(Level.OK);
        assertThat(line("Último error").value()).contains("wago down");
    }

    @Test
    void noBuildServedIsAnError() {
        when(gameDataService.currentBuild()).thenReturn(Optional.empty());
        when(gameDataService.currentGenerated()).thenReturn(Optional.empty());
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(null, null, null, null, false, null));

        assertThat(level("Build servida")).isEqualTo(Level.ERROR);
        assertThat(level("Última comprobación")).isEqualTo(Level.WARNING);
    }

    @Test
    void aBuildBeingTriedIsAWarningAndOneLeftAloneIsAnError() {
        served("1.60.1.70707");
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70938", null, null, false,
                new PendingBuild("1.60.1.70938", "abc", "", 1, null)));

        assertThat(level("Build pendiente")).isEqualTo(Level.WARNING);
        assertThat(line("Build pendiente").value()).isEqualTo("1.60.1.70938 · intento 1 de 2 · tumbó el backend");

        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70938", null, null, false,
                new PendingBuild("1.60.1.70938", "abc", "", 2, "no column PosX")));

        assertThat(level("Build pendiente")).isEqualTo(Level.ERROR);
        assertThat(line("Build pendiente").value())
                .isEqualTo("1.60.1.70938 · abandonada tras 2 intentos · no column PosX");
    }

    @Test
    void showsTheClassicEraBuildTheDataIsComparedWith() {
        served("1.60.1.70707");
        when(gameDataService.currentClassicBuild()).thenReturn(Optional.of("1.15.9.70576"));
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", null, null, false,
                null));

        assertThat(line("Referencia de Classic Era").value()).isEqualTo("1.15.9.70576");
        assertThat(line("Build pendiente").value()).isEqualTo("Ninguna");
    }

    private void served(String build) {
        when(gameDataService.currentBuild()).thenReturn(Optional.of(build));
        when(gameDataService.currentGenerated()).thenReturn(Optional.of("2026-10-30"));
    }

    private Level level(String label) {
        return line(label).level();
    }

    private ModuleStatus.Line line(String label) {
        return status.moduleStatus().lines().stream().filter(line -> line.label().equals(label)).findFirst().orElseThrow();
    }
}
