package com.adrianpmdev.wowforever.features.progress.application.service;

import com.adrianpmdev.platform.port.CurrentUser;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.adrianpmdev.wowforever.features.progress.api.dto.request.LegacyChallengesRequest;
import com.adrianpmdev.wowforever.features.progress.api.dto.request.SavedBuildRequest;
import com.adrianpmdev.wowforever.features.progress.api.dto.response.SavedBuildResponse;
import com.adrianpmdev.wowforever.features.progress.infrastructure.persistence.PlayerProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Everything acts on the caller, with the limits of the slots and the challenges.
 *
 * @author adrianpmdev
 */
class PlayerProgressServiceTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final SavedBuildRequest BUILD = new SavedBuildRequest(" Raid ", 60, "161.5-162.3");

    private final PlayerProgressRepository repository = mock(PlayerProgressRepository.class);
    private final GameDataService gameData = mock(GameDataService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final PlayerProgressService service = new PlayerProgressService(repository, gameData, currentUser);

    @BeforeEach
    void setUp() {
        when(currentUser.id()).thenReturn(Optional.of(ANA));
        when(gameData.getIndex()).thenReturn("{\"classes\":[{\"slug\":\"warrior\"}]}");
    }

    @Test
    void theBuildsAreAlwaysFiveSlotsWithTheEmptyOnesAsNull() {
        SavedBuildResponse kept = new SavedBuildResponse("Raid", 60, "161.5");
        when(repository.builds(ANA, "warrior")).thenReturn(Map.of(2, kept));

        assertThat(service.builds("warrior")).containsExactly(null, null, kept, null, null);
    }

    @Test
    void aBuildIsSavedOnTheCallerWithTheNameTrimmed() {
        when(repository.builds(ANA, "warrior")).thenReturn(Map.of());

        service.saveBuild("warrior", 4, BUILD);

        verify(repository).saveBuild(ANA, "warrior", 4, "Raid", 60, "161.5-162.3");
    }

    @Test
    void aSlotOutOfRangeIsRefused() {
        assertThatThrownBy(() -> service.saveBuild("warrior", 5, BUILD)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.deleteBuild("warrior", -1)).isInstanceOf(ResponseStatusException.class);

        verify(repository, never()).saveBuild(any(), any(), anyInt(), any(), anyInt(), any());
    }

    @Test
    void aClassTheGameDoesNotHaveIsRefused() {
        assertThatThrownBy(() -> service.saveBuild("nope", 0, BUILD))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void aSlotOfAClassTheGameRemovedCanStillBeEmptied() {
        when(repository.builds(ANA, "gone")).thenReturn(Map.of());

        service.deleteBuild("gone", 1);

        verify(repository).deleteBuild(ANA, "gone", 1);
    }

    @Test
    void challengesAreTickedAndUntickedOnTheCaller() {
        when(repository.challenges(ANA)).thenReturn(List.of());

        service.setChallenges(new LegacyChallengesRequest(List.of(" A ", "A", "B"), true));
        verify(repository).addChallenges(ANA, List.of("A", "B"));

        service.setChallenges(new LegacyChallengesRequest(List.of("A"), false));
        verify(repository).removeChallenges(ANA, List.of("A"));
    }

    @Test
    void tooManyChallengesAreRefused() {
        when(repository.challenges(ANA)).thenReturn(Collections.nCopies(PlayerProgressService.MAX_CHALLENGES, "X"));

        assertThatThrownBy(() -> service.setChallenges(new LegacyChallengesRequest(List.of("Y"), true)))
                .isInstanceOf(ResponseStatusException.class);

        verify(repository, never()).addChallenges(any(), anyCollection());
    }

    @Test
    void withoutACallerNothingHappens() {
        when(currentUser.id()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.builds("warrior")).isInstanceOf(ResponseStatusException.class);
    }
}
