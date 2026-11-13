package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.export.GameDataExporter;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.GameDataStore;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage.PendingBuild;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.WagoClient;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.WagoUnavailableException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * When the game data is restored, read or kept as it is: a new build is only read on a clean start (the backend
 * restarts to get one and again after the read), a build is never read without its note, and a build whose reads keep
 * failing is left alone instead of stopping the backend again and again.
 *
 * @author adrianpmdev
 */
class GameDataSyncServiceTest {

    private static final Instant START = Instant.parse("2026-11-13T02:00:00Z");
    private static final String KEPT_BUILD = GameDataFixtures.testData().path("build").asText();
    private static final String NEW_BUILD = "9.9.9.9";
    private static final String CLASSIC_BUILD = "1.15.9.70576";
    private static final String DEPLOY = "commit-a";

    private final GameDataService gameDataService = new GameDataService();
    private final String fingerprint = GameDataExporter.fingerprint(gameDataService.getConfig());
    private final WagoClient wagoClient = mock(WagoClient.class);
    private final GameDataStore store = mock(GameDataStore.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final BackendRestart backendRestart = mock(BackendRestart.class);
    private final GameDataExporter exporter = mock(GameDataExporter.class);
    private final MovingClock clock = new MovingClock(START);
    private final GameDataSyncService sync = service(true);

    @BeforeEach
    void keptCopyAndClassicReference() {
        when(store.load()).thenReturn(Optional.of(kept(KEPT_BUILD)));
        when(store.loadClassic()).thenReturn(Optional.of(classicReference()));
        when(store.pending()).thenReturn(Optional.empty());
    }

    /* ─── Nothing to read ─── */

    @Test
    void servesTheKeptCopyAndReadsNothingWhenItIsTheNewest() throws IOException {
        newest(KEPT_BUILD);

        sync.refresh();

        assertThat(gameDataService.currentBuild()).contains(KEPT_BUILD);
        verify(wagoClient, never()).tablesOf(anyString());
        verify(store, never()).save(any());
        verify(backendRestart, never()).run();
    }

    @Test
    void aNoteLeftBehindIsRemovedOnceItsBuildIsServed() throws IOException {
        newest(KEPT_BUILD);
        when(store.pending()).thenReturn(Optional.of(new PendingBuild(KEPT_BUILD, fingerprint, DEPLOY, 1, null)));

        sync.refresh();

        verify(store).deletePending();
        verify(backendRestart, never()).run();
    }

    /* ─── A new build ─── */

    @Test
    void aBackendThatHasRunAWhileNotesTheNewBuildAndRestartsToReadIt() throws IOException {
        newest(NEW_BUILD);
        clock.advance(Duration.ofHours(3));

        sync.refresh();

        verify(store).savePending(new PendingBuild(NEW_BUILD, fingerprint, DEPLOY, 0, null));
        verify(backendRestart).run();
        verify(wagoClient, never()).tablesOf(anyString());
        assertThat(gameDataService.currentBuild()).contains(KEPT_BUILD);
    }

    @Test
    void aBuildThatCannotBeNotedDoesNotRestartTheBackend() throws IOException {
        newest(NEW_BUILD);
        clock.advance(Duration.ofHours(3));
        doThrow(new IllegalStateException("Storage answered 500")).when(store).savePending(any());

        sync.refresh();

        verify(backendRestart, never()).run();
    }

    @Test
    void aCleanStartReadsTheBuildKeepsItRemovesTheNoteAndRestarts() throws IOException {
        newest(NEW_BUILD);
        exports(NEW_BUILD);

        sync.refresh();

        assertThat(gameDataService.currentBuild()).contains(NEW_BUILD);
        verify(store).savePending(new PendingBuild(NEW_BUILD, fingerprint, DEPLOY, 1, null));
        verify(store).save(any());
        verify(store).deletePending();
        verify(backendRestart).run();
        verify(exporter, never()).classicReference(any());
    }

    @Test
    void aBuildIsNeverReadWithoutItsNote() throws IOException {
        newest(NEW_BUILD);
        doThrow(new IllegalStateException("Storage answered 500")).when(store).savePending(any());

        sync.refresh();

        verify(exporter, never()).export(any(), any(), any());
        verify(backendRestart, never()).run();
        assertThat(gameDataService.currentBuild()).contains(KEPT_BUILD);
    }

    @Test
    void aKeptCopyWrittenByAnotherVersionOfTheExportIsServedAndItsBuildReadAgain() throws IOException {
        ObjectNode older = kept(KEPT_BUILD);
        older.put("exporter", "another-version");
        when(store.load()).thenReturn(Optional.of(older));
        newest(KEPT_BUILD);
        exports(KEPT_BUILD);

        sync.refresh();

        verify(exporter).export(any(), any(), any());
        assertThat(gameDataService.currentExporter()).isEqualTo(fingerprint);
        verify(backendRestart).run();
    }

    /* ─── The Classic reference ─── */

    @Test
    void theClassicReferenceIsReadOnceFromTheClientAndKept() throws IOException {
        newest(NEW_BUILD);
        exports(NEW_BUILD);
        when(store.loadClassic()).thenReturn(Optional.empty());
        when(wagoClient.latestBuild("wow_classic_era")).thenReturn(Optional.of(CLASSIC_BUILD));
        ObjectNode reference = classicReference();
        when(exporter.classicReference(any())).thenReturn(reference);

        sync.refresh();

        verify(store).saveClassic(reference);
        verify(exporter).export(any(), any(), any());
    }

    @Test
    void aClassicReferenceWrittenByAnotherVersionOfTheExportIsReadAgain() throws IOException {
        newest(NEW_BUILD);
        exports(NEW_BUILD);
        ObjectNode older = classicReference();
        older.put("exporter", "another-version");
        when(store.loadClassic()).thenReturn(Optional.of(older));
        when(wagoClient.latestBuild("wow_classic_era")).thenReturn(Optional.of(CLASSIC_BUILD));
        when(exporter.classicReference(any())).thenReturn(classicReference());

        sync.refresh();

        verify(exporter).classicReference(any());
        verify(store).saveClassic(any());
    }

    /* ─── Reads that fail ─── */

    @Test
    void aReadThatStoppedTheBackendIsTriedOnceMore() throws IOException {
        newest(NEW_BUILD);
        exports(NEW_BUILD);
        when(store.pending()).thenReturn(Optional.of(new PendingBuild(NEW_BUILD, fingerprint, DEPLOY, 1, null)));

        sync.refresh();

        verify(store).savePending(new PendingBuild(NEW_BUILD, fingerprint, DEPLOY, 2, null));
        assertThat(gameDataService.currentBuild()).contains(NEW_BUILD);
    }

    @Test
    void aBuildThatFailedTwiceIsLeftAloneAndShownOnTheDashboard() throws IOException {
        newest(NEW_BUILD);
        PendingBuild left = new PendingBuild(NEW_BUILD, fingerprint, DEPLOY, 2, null);
        when(store.pending()).thenReturn(Optional.of(left));

        sync.refresh();
        clock.advance(Duration.ofHours(3));
        sync.refresh();

        verify(exporter, never()).export(any(), any(), any());
        verify(store, never()).savePending(any());
        verify(backendRestart, never()).run();
        assertThat(sync.progress().pending()).isEqualTo(left);
    }

    @Test
    void aNewDeployTriesALeftBuildAgain() throws IOException {
        newest(NEW_BUILD);
        exports(NEW_BUILD);
        when(store.pending()).thenReturn(Optional.of(new PendingBuild(NEW_BUILD, fingerprint, "commit-before", 2, null)));

        sync.refresh();

        verify(store).savePending(new PendingBuild(NEW_BUILD, fingerprint, DEPLOY, 1, null));
        assertThat(gameDataService.currentBuild()).contains(NEW_BUILD);
    }

    @Test
    void aReadThatFailsCountsAsAnAttemptAndKeepsTheServedBuild() throws IOException {
        newest(NEW_BUILD);
        when(exporter.export(any(), any(), any())).thenThrow(new IllegalStateException("no column PosX"));

        assertThatThrownBy(sync::refresh).isInstanceOf(IllegalStateException.class);

        verify(store).savePending(new PendingBuild(NEW_BUILD, fingerprint, DEPLOY, 1, "no column PosX"));
        verify(store, never()).save(any());
        verify(backendRestart, never()).run();
        assertThat(gameDataService.currentBuild()).contains(KEPT_BUILD);
    }

    @Test
    void aReadThatFailsBecauseWagoWasUnavailableDoesNotCount() throws IOException {
        newest(NEW_BUILD);
        when(exporter.export(any(), any(), any())).thenThrow(new java.io.UncheckedIOException("table SpellName",
                new WagoUnavailableException("wago.tools answered 502", null)));

        assertThatThrownBy(sync::refresh).isInstanceOf(RuntimeException.class);

        verify(store).savePending(new PendingBuild(NEW_BUILD, fingerprint, DEPLOY, 0, "table SpellName"));
    }

    @Test
    void aCopyThatCouldNotBeKeptIsKeptOnTheNextCheckBeforeRestarting() throws IOException {
        newest(NEW_BUILD);
        exports(NEW_BUILD);
        doThrow(new IllegalStateException("Storage answered 500")).doNothing().when(store).save(any());

        sync.refresh();

        verify(backendRestart, never()).run();
        verify(store, never()).deletePending();

        clock.advance(Duration.ofHours(1));
        sync.refresh();

        verify(store, times(2)).save(any());
        verify(store).deletePending();
        verify(backendRestart).run();
        verify(exporter, times(1)).export(any(), any(), any());
    }

    /* ─── A developer machine ─── */

    @Test
    void aDeveloperMachineReadsInPlaceAndNeverWritesToStorageNorRestarts() throws IOException {
        GameDataSyncService local = service(false);
        newest(NEW_BUILD);
        exports(NEW_BUILD);
        clock.advance(Duration.ofHours(3));

        local.refresh();

        assertThat(gameDataService.currentBuild()).contains(NEW_BUILD);
        verify(store, never()).savePending(any());
        verify(store, never()).save(any());
        verify(store, never()).saveClassic(any());
        verify(backendRestart, never()).run();
    }

    /* ─── When the checks run ─── */

    @Test
    void checksAgainSoonWhileNothingIsServedOrAFreshStartFailsAndHourlyAfterwards() throws IOException {
        when(store.load()).thenReturn(Optional.empty()).thenReturn(Optional.of(kept(KEPT_BUILD)));
        when(wagoClient.latestBuild(anyString())).thenThrow(new WagoUnavailableException("unreachable", null));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> at = ArgumentCaptor.forClass(Instant.class);

        sync.start();
        verify(scheduler).schedule(task.capture(), at.capture());
        task.getValue().run();
        verify(scheduler, times(2)).schedule(task.capture(), at.capture());
        task.getValue().run();
        verify(scheduler, times(3)).schedule(task.capture(), at.capture());
        clock.advance(Duration.ofMinutes(20));
        task.getValue().run();
        verify(scheduler, times(4)).schedule(task.capture(), at.capture());

        List<Instant> times = at.getAllValues();
        assertThat(times.getFirst()).isEqualTo(START);
        assertThat(times.get(times.size() - 3)).isEqualTo(START.plus(Duration.ofMinutes(5)));
        assertThat(times.get(times.size() - 2)).isEqualTo(START.plus(Duration.ofMinutes(5)));
        assertThat(times.getLast()).isEqualTo(START.plus(Duration.ofMinutes(80)));
        assertThat(gameDataService.currentBuild()).contains(KEPT_BUILD);
    }

    @Test
    void noCheckIsPlannedOnceTheBackendRestarts() throws IOException {
        newest(NEW_BUILD);
        clock.advance(Duration.ofHours(3));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);

        sync.start();
        verify(scheduler).schedule(task.capture(), any(Instant.class));
        task.getValue().run();

        verify(backendRestart).run();
        verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }

    /* ─── Helpers ─── */

    private GameDataSyncService service(boolean keepCopy) {
        return new GameDataSyncService(gameDataService, wagoClient, store, scheduler, backendRestart, clock, keepCopy,
                DEPLOY, config -> exporter);
    }

    private void newest(String build) throws IOException {
        when(wagoClient.latestBuild("wowf")).thenReturn(Optional.of(build));
    }

    private void exports(String build) {
        when(exporter.export(any(), any(), any())).thenReturn(kept(build));
    }

    private ObjectNode kept(String build) {
        ObjectNode data = GameDataFixtures.testData();
        data.put("build", build);
        data.put("exporter", fingerprint);

        return data;
    }

    private ObjectNode classicReference() {
        ObjectNode reference = GameDataFixtures.testData().objectNode();
        reference.put("exporter", fingerprint);
        reference.put("build", CLASSIC_BUILD);
        reference.putObject("classes").putObject("Warrior");

        return reference;
    }

    /** A clock the test moves forward. */
    private static final class MovingClock extends Clock {

        private Instant now;

        private MovingClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
