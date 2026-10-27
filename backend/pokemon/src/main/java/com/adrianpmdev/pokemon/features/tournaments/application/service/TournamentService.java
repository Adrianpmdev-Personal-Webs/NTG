package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity.Game;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.repository.GameRepository;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeRepository;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentFormat;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentStatus;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Tournaments: create (with the roster of the randomlocke), edit and delete (with their team pictures).
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final LockeRepository lockeRepository;
    private final GameRepository gameRepository;
    private final TeamImageService teamImageService;
    private final TournamentQueryService queryService;

    /**
     * Creates a tournament at the end of its randomlocke, in draft, with the roster as players.
     *
     * @param request tournament data
     * @return the new tournament
     */
    @Transactional
    public TournamentDetailResponse create(TournamentCreateRequest request) {
        Locke locke = lockeRepository.findWithRosterById(request.lockeId()).orElseThrow(() -> ApiErrors.notFound("Locke"));

        Tournament tournament = new Tournament();
        tournament.setLocke(locke);
        tournament.setGame(findGame(request.gameId()));
        tournament.setPosition((short) (tournamentRepository.findLastPosition(locke.getId()) + 1));
        tournament.setName(request.name().trim());
        tournament.setDescription(blankToNull(request.description()));
        tournament.setFormat(TournamentFormat.fromValue(request.format()));
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setPointsPerWin((short) request.pointsPerWin());
        tournament.setTeamsRevealAt(request.teamsRevealAt());
        tournamentRepository.save(tournament);

        locke.getRoster().forEach(player -> {
            TournamentPlayer entry = new TournamentPlayer();
            entry.setTournament(tournament);
            entry.setPlayer(player);
            tournamentPlayerRepository.save(entry);
        });

        return queryService.detail(tournament.getId());
    }

    /**
     * Replaces the data of a tournament.
     *
     * @param id      tournament id
     * @param request tournament data
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse update(UUID id, TournamentUpdateRequest request) {
        Tournament tournament = tournamentRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Tournament"));
        tournament.setGame(findGame(request.gameId()));
        tournament.setName(request.name().trim());
        tournament.setDescription(blankToNull(request.description()));
        tournament.setStatus(TournamentStatus.fromValue(request.status()));
        tournament.setPointsPerWin((short) request.pointsPerWin());
        tournament.setTeamsRevealAt(request.teamsRevealAt());
        tournamentRepository.save(tournament);

        return queryService.detail(id);
    }

    /**
     * Deletes a tournament with everything in it, and its team pictures.
     *
     * @param id tournament id
     */
    @Transactional
    public void delete(UUID id) {
        Tournament tournament = tournamentRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Tournament"));
        List<String> pictures = tournamentPlayerRepository.findTeamImages(List.of(id));

        // The database cascades the delete to players, series, games and rewards.
        tournamentRepository.delete(tournament);
        tournamentRepository.flush();
        teamImageService.deleteQuietly(pictures);
    }

    private Game findGame(UUID id) {
        return gameRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Game"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
