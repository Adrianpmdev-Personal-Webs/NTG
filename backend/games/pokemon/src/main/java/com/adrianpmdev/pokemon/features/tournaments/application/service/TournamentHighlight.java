package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.platform.module.GameHighlightProvider;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentStatus;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Tells on the home page how many tournaments are being played, with one small count.
 *
 * @author adrianpmdev
 */
@Component
public class TournamentHighlight implements GameHighlightProvider {

    private final TournamentRepository tournamentRepository;

    /**
     * Builds the provider.
     *
     * @param tournamentRepository tournaments
     */
    public TournamentHighlight(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Override
    public String gameKey() {
        return "pokemon";
    }

    @Override
    public Optional<String> highlight() {
        long playing = tournamentRepository.countByStatus(TournamentStatus.IN_PROGRESS);

        if (playing == 0) {
            return Optional.empty();
        }

        return Optional.of(playing == 1 ? "Torneo en curso" : playing + " torneos en curso");
    }
}
