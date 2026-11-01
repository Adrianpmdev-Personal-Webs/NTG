package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentStatus;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The text of the home page for each number of tournaments being played.
 *
 * @author adrianpmdev
 */
class TournamentHighlightTest {

    private final TournamentRepository repository = mock(TournamentRepository.class);
    private final TournamentHighlight highlight = new TournamentHighlight(repository);

    @Test
    void nothingIsToldWithoutTournamentsBeingPlayed() {
        when(repository.countByStatus(TournamentStatus.IN_PROGRESS)).thenReturn(0L);

        assertThat(highlight.highlight()).isEmpty();
    }

    @Test
    void oneTournamentIsToldInSingular() {
        when(repository.countByStatus(TournamentStatus.IN_PROGRESS)).thenReturn(1L);

        assertThat(highlight.highlight()).contains("Torneo en curso");
    }

    @Test
    void severalTournamentsAreCounted() {
        when(repository.countByStatus(TournamentStatus.IN_PROGRESS)).thenReturn(3L);

        assertThat(highlight.highlight()).contains("3 torneos en curso");
    }
}
