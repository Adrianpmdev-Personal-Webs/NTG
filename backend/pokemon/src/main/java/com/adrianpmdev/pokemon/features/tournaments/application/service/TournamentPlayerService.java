package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.repository.PlayerRepository;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentPlayerUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentFormat;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.MatchRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Players inside a tournament: add, set group and points adjustment, remove.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class TournamentPlayerService {

    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final TeamImageService teamImageService;
    private final TournamentQueryService queryService;

    /**
     * Adds a player to a tournament.
     *
     * @param tournamentId tournament id
     * @param playerId     player id
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse add(UUID tournamentId, UUID playerId) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> ApiErrors.notFound("Tournament"));
        Player player = playerRepository.findById(playerId).orElseThrow(() -> ApiErrors.notFound("Player"));

        if (tournamentPlayerRepository.existsByTournamentIdAndPlayerId(tournamentId, playerId)) {
            throw ApiErrors.conflict("El jugador ya está en el torneo");
        }

        TournamentPlayer entry = new TournamentPlayer();
        entry.setTournament(tournament);
        entry.setPlayer(player);
        tournamentPlayerRepository.save(entry);

        return queryService.detail(tournamentId);
    }

    /**
     * Sets the group and the points adjustment of a player inside a tournament.
     *
     * @param id      tournament player id
     * @param request new data
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse update(UUID id, TournamentPlayerUpdateRequest request) {
        TournamentPlayer entry = find(id);

        if (request.group() != null && entry.getTournament().getFormat() != TournamentFormat.GROUPS_BRACKET) {
            throw ApiErrors.conflict("Una liga no tiene grupos");
        }

        entry.setGroupName(request.group());
        entry.setPointsAdjustment((short) request.pointsAdjustment());
        tournamentPlayerRepository.save(entry);

        return queryService.detail(entry.getTournament().getId());
    }

    /**
     * Removes a player from a tournament, with the series they were in. Refused when they have games.
     *
     * @param id tournament player id
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse remove(UUID id) {
        TournamentPlayer entry = find(id);

        if (matchRepository.playerHasGames(id)) {
            throw ApiErrors.conflict("El jugador ya tiene partidas registradas en este torneo");
        }

        UUID tournamentId = entry.getTournament().getId();
        String picture = entry.getTeamImage();

        // The database removes their series (on delete cascade) and frees the rewards they picked.
        tournamentPlayerRepository.delete(entry);
        tournamentPlayerRepository.flush();

        if (picture != null) {
            teamImageService.deleteQuietly(List.of(picture));
        }

        return queryService.detail(tournamentId);
    }

    private TournamentPlayer find(UUID id) {
        return tournamentPlayerRepository.findDetailedById(id).orElseThrow(() -> ApiErrors.notFound("Tournament player"));
    }
}
