package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.tournaments.domain.MatchStage;
import com.adrianpmdev.pokemon.features.tournaments.domain.SeriesScore;
import com.adrianpmdev.pokemon.features.tournaments.domain.Standings;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentStatus;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Match;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.MatchGame;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Standings of one tournament, computed from its entities with the pure {@link Standings} rules. Used by
 * the tournament detail, the randomlocke standings and the global ranking.
 *
 * @author adrianpmdev
 */
public final class TournamentResults {

    /**
     * Results of a tournament.
     *
     * @param table      every player, all series counted, points with the adjustment
     * @param groups     group tables (group series only, no adjustment), by group name
     * @param finalOrder tournament players from first to last (bracket placement when there is one)
     * @param champion   winner of a finished tournament, null otherwise
     */
    public record Result(List<Standings.Row> table, Map<String, List<Standings.Row>> groups, List<UUID> finalOrder,
                         UUID champion) {

        /**
         * Table rows in final order.
         *
         * @return rows of the table, sorted by the final order
         */
        public List<Standings.Row> rowsInFinalOrder() {
            Map<UUID, Standings.Row> byPlayer = table.stream()
                    .collect(Collectors.toMap(Standings.Row::participant, Function.identity()));

            return finalOrder.stream().map(byPlayer::get).toList();
        }
    }

    private TournamentResults() {
    }

    /**
     * Score of a series, counted from its games.
     *
     * @param match series
     * @return score
     */
    public static SeriesScore toScore(Match match) {
        int wins1 = 0;
        int wins2 = 0;

        for (MatchGame game : match.getGames()) {
            if (game.getWinnerSide() == 1) {
                wins1++;
            } else {
                wins2++;
            }
        }

        return new SeriesScore(match.getId(), match.getStage(), match.getRound(), match.getSlot(),
                idOf(match.getPlayer1()), idOf(match.getPlayer2()), wins1, wins2);
    }

    /**
     * Computes the results of a tournament.
     *
     * @param tournament the tournament
     * @param players    its players (with their player row)
     * @param matches    its series (with their games)
     * @return results
     */
    public static Result compute(Tournament tournament, List<TournamentPlayer> players, List<Match> matches) {
        Map<UUID, String> names = players.stream()
                .collect(Collectors.toMap(TournamentPlayer::getId, tp -> tp.getPlayer().getName()));
        Comparator<UUID> byName = Comparator.comparing(id -> names.getOrDefault(id, ""), String.CASE_INSENSITIVE_ORDER);
        Map<UUID, Integer> adjustments = players.stream()
                .collect(Collectors.toMap(TournamentPlayer::getId, tp -> (int) tp.getPointsAdjustment()));
        List<SeriesScore> scores = matches.stream().map(TournamentResults::toScore).toList();
        int pointsPerWin = tournament.getPointsPerWin();

        List<UUID> ids = players.stream().map(TournamentPlayer::getId).toList();
        List<Standings.Row> table = Standings.table(ids, scores, pointsPerWin, adjustments, byName);

        Map<String, List<Standings.Row>> groups = new TreeMap<>();
        players.stream()
                .map(TournamentPlayer::getGroupName)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(group -> {
                    List<UUID> members = players.stream()
                            .filter(tp -> group.equals(tp.getGroupName()))
                            .map(TournamentPlayer::getId)
                            .toList();
                    List<SeriesScore> groupSeries = scores.stream()
                            .filter(score -> score.stage() == MatchStage.GROUP)
                            .filter(score -> members.contains(score.player1()) && members.contains(score.player2()))
                            .toList();
                    groups.put(group, Standings.table(members, groupSeries, pointsPerWin, Map.of(), byName));
                });

        List<SeriesScore> bracket = scores.stream().filter(score -> score.stage().isBracket()).toList();
        List<UUID> finalOrder = Standings.finalOrder(table, bracket);
        UUID champion = tournament.getStatus() == TournamentStatus.FINISHED && !finalOrder.isEmpty()
                ? finalOrder.getFirst()
                : null;

        return new Result(table, new LinkedHashMap<>(groups), finalOrder, champion);
    }

    private static UUID idOf(TournamentPlayer player) {
        return player == null ? null : player.getId();
    }
}
