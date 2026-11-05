package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * When a round runs, and what it saves.
 *
 * @author adrianpmdev
 */
class CharacterArmorySyncTest {

    private static final ArmoryCharacter READ = new ArmoryCharacter("a", "Alba", "Mage", "Human", 20, List.of(), null);

    private final GameCharacterRepository repository = mock(GameCharacterRepository.class);
    private final CharacterArmory armory = mock(CharacterArmory.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final CharacterArmorySync sync = new CharacterArmorySync(repository, armory, new GameDataService(), scheduler);
    private final List<Runnable> planned = new ArrayList<>();

    @BeforeEach
    void setUp() {
        sync.pause = Duration.ZERO;
        when(repository.findAll()).thenReturn(List.of());
        when(scheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(call -> {
            planned.add(call.getArgument(0));
            return null;
        });
    }

    @Test
    void openingTheScreenStartsARoundAndWhoComesDuringItWaitsForTheSameOne() {
        CompletableFuture<Void> first = sync.refresh();
        CompletableFuture<Void> second = sync.refresh();

        assertThat(second).isSameAs(first);
        assertThat(first).isNotDone();
        runPlanned();
        assertThat(first).isCompleted();
        assertThat(planned).isEmpty();
    }

    @Test
    void openingTheScreenRightAfterARoundDoesNotStartAnother() {
        sync.refresh();
        runPlanned();

        assertThat(sync.refresh()).isCompleted();
        assertThat(planned).isEmpty();
    }

    @Test
    void theRoundAtDawnRunsWhenNobodyOpenedTheScreen() {
        sync.atDawn();

        assertThat(planned).hasSize(1);
    }

    @Test
    void theRoundAtDawnIsNotNeededAfterARecentOne() {
        sync.refresh();
        runPlanned();

        sync.atDawn();

        assertThat(planned).isEmpty();
    }

    @Test
    void aRoundThatFailedCanBeRepeatedRightAway() throws IOException {
        GameCharacter character = stored("Alba");
        when(repository.findAll()).thenReturn(List.of(character));
        when(armory.changesOf(character)).thenThrow(new IOException("down"));

        sync.refresh();
        runPlanned();
        sync.refresh();
        runPlanned();

        verify(armory, times(2)).changesOf(character);
    }

    @Test
    void onlyTheCharactersThatChangedAreSaved() throws IOException {
        GameCharacter changed = stored("Alba");
        GameCharacter same = stored("Zul");
        when(repository.findAll()).thenReturn(List.of(changed, same));
        when(armory.changesOf(changed)).thenReturn(Optional.of(READ));
        when(armory.changesOf(same)).thenReturn(Optional.empty());

        sync.refresh();
        runPlanned();

        verify(armory).apply(changed, READ);
        verify(repository).saveAndFlush(changed);
        verify(repository, never()).saveAndFlush(same);
    }

    @Test
    void anArmoryThatFailsEndsTheRoundWithoutTouchingAnything() throws IOException {
        GameCharacter first = stored("Alba");
        GameCharacter second = stored("Zul");
        when(repository.findAll()).thenReturn(List.of(first, second));
        when(armory.changesOf(first)).thenThrow(new IOException("down"));

        sync.refresh();
        runPlanned();

        verify(armory, never()).changesOf(second);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void aCharacterDeletedDuringTheReadIsNotBroughtBack() throws IOException {
        GameCharacter character = stored("Alba");
        when(repository.findAll()).thenReturn(List.of(character));
        when(armory.changesOf(character)).thenReturn(Optional.of(READ));
        when(repository.findById(character.getId())).thenReturn(Optional.empty());

        sync.refresh();
        runPlanned();

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void aCharacterRenamedDuringTheReadKeepsItsNewName() throws IOException {
        GameCharacter read = stored("Alba");
        GameCharacter fresh = new GameCharacter();
        fresh.setId(read.getId());
        fresh.setName("Nueva");
        when(repository.findAll()).thenReturn(List.of(read));
        when(armory.changesOf(read)).thenReturn(Optional.of(READ));
        when(repository.findById(read.getId())).thenReturn(Optional.of(fresh));

        sync.refresh();
        runPlanned();

        assertThat(fresh.getName()).isEqualTo("Nueva");
        verify(repository, never()).saveAndFlush(any());
    }

    private void runPlanned() {
        List<Runnable> rounds = List.copyOf(planned);

        planned.clear();
        rounds.forEach(Runnable::run);
    }

    private GameCharacter stored(String name) {
        GameCharacter character = new GameCharacter();
        character.setId(UUID.randomUUID());
        character.setName(name);
        when(repository.findById(character.getId())).thenReturn(Optional.of(character));
        return character;
    }
}
