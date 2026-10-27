package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.GroupStandingsResponse;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.MatchGameResponse;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.MatchResponse;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.RewardResponse;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.StandingResponse;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentPlayerResponse;
import com.adrianpmdev.pokemon.features.tournaments.domain.DoubleEliminationBracket;
import com.adrianpmdev.pokemon.features.tournaments.domain.DoubleEliminationBracket.Place;
import com.adrianpmdev.pokemon.features.tournaments.domain.SeriesScore;
import com.adrianpmdev.pokemon.features.tournaments.domain.Standings;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Match;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.MatchRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.RewardRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.shared.dto.RefResponse;
import com.adrianpmdev.platform.error.ApiErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the full page of a tournament: players, series, standings, group tables and rewards.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class TournamentQueryService {

    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final MatchRepository matchRepository;
    private final RewardRepository rewardRepository;
    private final TeamImageService teamImageService;

    /**
     * Full page of a tournament, as the caller may see it.
     *
     * @param id tournament id
     * @return tournament detail
     */
    @Transactional(readOnly = true)
    public TournamentDetailResponse detail(UUID id) {
        Tournament tournament = tournamentRepository.findWithLockeAndGameById(id)
                .orElseThrow(() -> ApiErrors.notFound("Tournament"));
        List<TournamentPlayer> players = tournamentPlayerRepository.findByTournament(id);
        List<Match> matches = matchRepository.findByTournament(id);
        TournamentResults.Result results = TournamentResults.compute(tournament, players, matches);

        return new TournamentDetailResponse(
                tournament.getId(),
                new RefResponse(tournament.getLocke().getId(), tournament.getLocke().getName()),
                new RefResponse(tournament.getGame().getId(), tournament.getGame().getName()),
                tournament.getPosition(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getFormat().value(),
                tournament.getStatus().value(),
                tournament.getPointsPerWin(),
                tournament.getTeamsRevealAt(),
                tournament.teamsRevealed(Instant.now()),
                tournament.getBracketSize() == null ? null : tournament.getBracketSize().intValue(),
                toPlayers(players),
                toMatches(tournament, matches),
                toStandings(results.rowsInFinalOrder()),
                results.groups().entrySet().stream()
                        .map(entry -> new GroupStandingsResponse(entry.getKey(), toStandings(entry.getValue())))
                        .toList(),
                rewardRepository.findByTournamentIdOrderBySortOrderAsc(id).stream()
                        .map(reward -> new RewardResponse(reward.getId(), reward.getSortOrder(), reward.getName(),
                                reward.getChosenBy() == null ? null : reward.getChosenBy().getId()))
                        .toList());
    }

    private List<TournamentPlayerResponse> toPlayers(List<TournamentPlayer> players) {
        List<TournamentPlayer> visible = players.stream().filter(teamImageService::canSee).toList();
        Map<String, String> urls = teamImageService.urlsFor(visible.stream().map(TournamentPlayer::getTeamImage).toList());

        return players.stream()
                .sorted(Comparator.comparing(tp -> tp.getPlayer().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(tp -> new TournamentPlayerResponse(
                        tp.getId(),
                        tp.getPlayer().getId(),
                        tp.getPlayer().getName(),
                        tp.getGroupName(),
                        tp.getPointsAdjustment(),
                        tp.getTeamImage() != null,
                        visible.contains(tp) && tp.getTeamImage() != null ? urls.get(tp.getTeamImage()) : null,
                        teamImageService.isOwner(tp),
                        teamImageService.canEdit(tp)))
                .toList();
    }

    private static List<MatchResponse> toMatches(Tournament tournament, List<Match> matches) {
        Map<Place, DoubleEliminationBracket.Resolved> bracket = resolveBracket(tournament, matches);
        List<MatchResponse> result = new ArrayList<>();

        matches.stream()
                .sorted(Comparator.comparing((Match m) -> m.getStage().ordinal())
                        .thenComparing(Match::getRound)
                        .thenComparing(Match::getSlot))
                .forEach(match -> {
                    SeriesScore score = TournamentResults.toScore(match);
                    DoubleEliminationBracket.Resolved resolved = bracket.get(placeOf(match));
                    String status = resolved != null
                            ? resolved.state().name().toLowerCase(Locale.ROOT)
                            : simpleStatus(score);

                    result.add(new MatchResponse(
                            match.getId(),
                            match.getStage().value(),
                            match.getRound(),
                            match.getSlot(),
                            score.player1(),
                            score.player2(),
                            score.wins1(),
                            score.wins2(),
                            score.winner(),
                            status,
                            match.getGames().stream()
                                    .map(game -> new MatchGameResponse(game.getGameNumber(), game.getWinnerSide(),
                                            game.getReplayUrl(), game.getPlayedAt()))
                                    .toList()));
                });

        return result;
    }

    /** States of a generated bracket (byes and void series); imported brackets have none. */
    private static Map<Place, DoubleEliminationBracket.Resolved> resolveBracket(Tournament tournament, List<Match> matches) {
        if (tournament.getBracketSize() == null) {
            return Map.of();
        }

        Map<Place, SeriesScore> scores = matches.stream()
                .filter(match -> match.getStage().isBracket())
                .collect(Collectors.toMap(TournamentQueryService::placeOf, TournamentResults::toScore));

        return DoubleEliminationBracket.ofSize(tournament.getBracketSize()).resolve(scores);
    }

    private static String simpleStatus(SeriesScore score) {
        if (score.player1() == null || score.player2() == null) {
            return "pending";
        }

        return score.decided() ? "finished" : "ready";
    }

    static Place placeOf(Match match) {
        return new Place(match.getStage(), match.getRound(), match.getSlot());
    }

    private static List<StandingResponse> toStandings(List<Standings.Row> rows) {
        List<StandingResponse> standings = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Standings.Row row = rows.get(i);
            standings.add(new StandingResponse(row.participant(), i + 1, row.played(), row.wins(), row.losses(),
                    row.gamesWon(), row.gamesLost(), row.points()));
        }

        return standings;
    }
}
