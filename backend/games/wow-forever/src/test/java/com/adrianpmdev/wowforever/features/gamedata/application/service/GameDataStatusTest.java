package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.platform.status.ModuleStatus;
import com.adrianpmdev.platform.status.ModuleStatus.Level;
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
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", null, null, false));

        assertThat(status.moduleStatus().lines()).allSatisfy(line -> assertThat(line.level()).isEqualTo(Level.OK));
    }

    @Test
    void aNewerBuildInWagoIsAWarning() {
        served("1.60.1.70685");
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", null, null, false));

        assertThat(level("Build más nueva en wago")).isEqualTo(Level.WARNING);
    }

    @Test
    void aFailingCheckIsAnErrorWithItsMessage() {
        served("1.60.1.70707");
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", "wago down", NOW, true));

        assertThat(level("Último error")).isEqualTo(Level.ERROR);
        assertThat(line("Último error").value()).contains("wago down");
    }

    @Test
    void anErrorThatPassedStaysVisibleButIsNotHighlighted() {
        served("1.60.1.70707");
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(NOW, "1.60.1.70707", "wago down", NOW, false));

        assertThat(level("Último error")).isEqualTo(Level.OK);
        assertThat(line("Último error").value()).contains("wago down");
    }

    @Test
    void noBuildServedIsAnError() {
        when(gameDataService.currentBuild()).thenReturn(Optional.empty());
        when(gameDataService.currentGenerated()).thenReturn(Optional.empty());
        when(syncService.progress()).thenReturn(new GameDataSyncService.Progress(null, null, null, null, false));

        assertThat(level("Build servida")).isEqualTo(Level.ERROR);
        assertThat(level("Última comprobación")).isEqualTo(Level.WARNING);
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
