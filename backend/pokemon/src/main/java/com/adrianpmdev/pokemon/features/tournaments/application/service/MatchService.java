package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.MatchCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.MatchGamesRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.domain.MatchStage;
import com.adrianpmdev.pokemon.features.tournaments.domain.SeriesScore;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Match;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.MatchGame;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.MatchRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Series: record their games, and add or remove a series by hand.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class MatchService {

    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final MatchRepository matchRepository;
    private final ScheduleService scheduleService;
    private final TournamentQueryService queryService;

    /**
     * Replaces the games of a series. The series ends as soon as a side has two wins, so no game may
     * follow that. In a generated bracket the later series are updated afterwards.
     *
     * @param matchId series id
     * @param request games in order
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse recordGames(UUID matchId, MatchGamesRequest request) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> ApiErrors.notFound("Match"));

        if (!request.games().isEmpty() && (match.getPlayer1() == null || match.getPlayer2() == null)) {
            throw ApiErrors.conflict("La serie todavía no tiene dos jugadores");
        }

        requireValidSeries(request.games());

        Map<Integer, Instant> playedAt = new HashMap<>();
        match.getGames().forEach(game -> playedAt.put((int) game.getGameNumber(), game.getPlayedAt()));
        match.getGames().clear();
        matchRepository.flush();

        for (int i = 0; i < request.games().size(); i++) {
            MatchGamesRequest.Game input = request.games().get(i);
            MatchGame game = new MatchGame();
            game.setMatch(match);
            game.setGameNumber((short) (i + 1));
            game.setWinnerSide((short) input.winnerSide());
            game.setReplayUrl(input.replayUrl() == null || input.replayUrl().isBlank() ? null : input.replayUrl().trim());
            // Editing a series keeps when its games were first recorded.
            game.setPlayedAt(playedAt.getOrDefault(i + 1, Instant.now()));
            match.getGames().add(game);
        }

        matchRepository.save(match);
        Tournament tournament = match.getTournament();

        if (match.getStage().isBracket()) {
            scheduleService.syncBracket(tournament);
        }

        return queryService.detail(tournament.getId());
    }

    /**
     * Adds a series by hand.
     *
     * @param tournamentId tournament id
     * @param request      series data
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse create(UUID tournamentId, MatchCreateRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> ApiErrors.notFound("Tournament"));
        TournamentPlayer player1 = findEntryOf(tournamentId, request.player1Id());
        TournamentPlayer player2 = findEntryOf(tournamentId, request.player2Id());

        if (player1.getId().equals(player2.getId())) {
            throw ApiErrors.badRequest("Un jugador no puede jugar contra sí mismo");
        }

        Match match = new Match();
        match.setTournament(tournament);
        match.setStage(MatchStage.fromValue(request.stage()));
        match.setRound((short) request.round());
        match.setSlot((short) request.slot());
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        // Flush so a taken stage/round/slot fails here, as a 409.
        matchRepository.saveAndFlush(match);

        return queryService.detail(tournamentId);
    }

    /**
     * Deletes a series and its games.
     *
     * @param matchId series id
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse delete(UUID matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> ApiErrors.notFound("Match"));
        UUID tournamentId = match.getTournament().getId();

        if (match.getTournament().getBracketSize() != null && match.getStage().isBracket()) {
            throw ApiErrors.conflict("Las series de un bracket generado no se borran: genera el bracket de nuevo");
        }

        matchRepository.delete(match);

        return queryService.detail(tournamentId);
    }

    private static void requireValidSeries(List<MatchGamesRequest.Game> games) {
        int wins1 = 0;
        int wins2 = 0;

        for (MatchGamesRequest.Game game : games) {
            if (wins1 >= SeriesScore.WINS_NEEDED || wins2 >= SeriesScore.WINS_NEEDED) {
                throw ApiErrors.badRequest("La serie ya estaba decidida: sobran partidas");
            }

            if (game.winnerSide() == 1) {
                wins1++;
            } else {
                wins2++;
            }
        }
    }

    private TournamentPlayer findEntryOf(UUID tournamentId, UUID tournamentPlayerId) {
        TournamentPlayer entry = tournamentPlayerRepository.findById(tournamentPlayerId)
                .orElseThrow(() -> ApiErrors.notFound("Tournament player"));

        if (!entry.getTournament().getId().equals(tournamentId)) {
            throw ApiErrors.badRequest("El jugador no pertenece a este torneo");
        }

        return entry;
    }
}
