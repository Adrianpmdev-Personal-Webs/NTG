package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.GameDataStore;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.WagoClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * When the game data is restored, rebuilt or kept as it is.
 *
 * @author adrianpmdev
 */
class GameDataSyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
    private static final String KEPT_BUILD = GameDataFixtures.testData().path("build").asText();

    private final GameDataService gameDataService = new GameDataService();
    private final WagoClient wagoClient = mock(WagoClient.class);
    private final GameDataStore store = mock(GameDataStore.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final GameDataSyncService sync = new GameDataSyncService(gameDataService, wagoClient, store, scheduler,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void servesTheKeptCopyAndSkipsABuildItAlreadyHas() throws IOException {
        when(store.load()).thenReturn(Optional.of(GameDataFixtures.testData()));
        when(wagoClient.latestBuild("wowf")).thenReturn(Optional.of(KEPT_BUILD));

        sync.refresh();

        assertThat(gameDataService.currentBuild()).contains(KEPT_BUILD);
        verify(wagoClient, never()).tablesOf(anyString());
        verify(store, never()).save(any());
    }

    @Test
    void aBuildThatCannotBeReadNeverReplacesTheServedOne() throws IOException {
        when(store.load()).thenReturn(Optional.of(GameDataFixtures.testData()));
        when(wagoClient.latestBuild(anyString())).thenReturn(Optional.of("9.9.9.9"));
        when(wagoClient.tablesOf(anyString())).thenReturn((name, columns, keep) -> {
            throw new IOException("wago.tools answered 500");
        });

        assertThatThrownBy(sync::refresh).isInstanceOf(RuntimeException.class);

        assertThat(gameDataService.currentBuild()).contains(KEPT_BUILD);
        verify(store, never()).save(any());
    }

    @Test
    void checksAgainSoonWhileNothingIsServedAndHourlyAfterwards() throws IOException {
        when(store.load()).thenReturn(Optional.empty()).thenReturn(Optional.of(GameDataFixtures.testData()));
        when(wagoClient.latestBuild(anyString())).thenThrow(new IOException("unreachable"));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> when = ArgumentCaptor.forClass(Instant.class);

        sync.start();
        verify(scheduler).schedule(task.capture(), when.capture());
        task.getValue().run();
        verify(scheduler, times(2)).schedule(task.capture(), when.capture());
        task.getValue().run();
        verify(scheduler, times(3)).schedule(task.capture(), when.capture());

        List<Instant> times = when.getAllValues();
        assertThat(times.getFirst()).isEqualTo(NOW);
        assertThat(times.get(times.size() - 2)).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(times.getLast()).isEqualTo(NOW.plus(Duration.ofMinutes(60)));
        assertThat(gameDataService.currentBuild()).contains(KEPT_BUILD);
    }
}
