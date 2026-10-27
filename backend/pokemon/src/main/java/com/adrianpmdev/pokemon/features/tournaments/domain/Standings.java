package com.adrianpmdev.pokemon.features.tournaments.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Standings rules. Only decided series count.
 *
 * <p>Table order: points, then game difference, then head-to-head among the tied players (the direct
 * series when two are tied, the wins among them when more are tied), then the given last tie-break
 * (the player name).
 *
 * <p>Final order of a tournament with a bracket: champion, finalist, then the rest by how late they were
 * knocked out in the lower side; players still alive go before the knocked out ones. Players at the same
 * level keep the table order.
 *
 * @author adrianpmdev
 */
public final class Standings {

    /**
     * One row of a standings table.
     *
     * @param participant tournament player id
     * @param played      decided series played
     * @param wins        series won
     * @param losses      series lost
     * @param gamesWon    games won in decided series
     * @param gamesLost   games lost in decided series
     * @param points      series won x points per win + adjustment
     */
    public record Row(UUID participant, int played, int wins, int losses, int gamesWon, int gamesLost, int points) {

        /**
         * Games won minus games lost.
         *
         * @return game difference
         */
        public int gameDifference() {
            return gamesWon - gamesLost;
        }
    }

    private static final int CHAMPION = 1_000;
    private static final int FINALIST = 900;
    private static final int ALIVE = 500;
    private static final int KNOCKED_OUT = 100;
    private static final int NOT_IN_BRACKET = 0;

    private Standings() {
    }

    /**
     * Builds a standings table.
     *
     * @param participants  tournament players to rank (players without series get an empty row)
     * @param series        series to count (only decided ones count)
     * @param pointsPerWin  points per series won
     * @param adjustments   extra points per tournament player (may be empty)
     * @param lastTieBreak  order used when everything else is equal
     * @return rows in standings order
     */
    public static List<Row> table(Collection<UUID> participants, Collection<SeriesScore> series, int pointsPerWin,
                                  Map<UUID, Integer> adjustments, Comparator<UUID> lastTieBreak) {
        Map<UUID, int[]> stats = new LinkedHashMap<>();
        participants.forEach(participant -> stats.put(participant, new int[5]));

        List<SeriesScore> decided = series.stream().filter(SeriesScore::decided).toList();

        for (SeriesScore score : decided) {
            for (UUID player : List.of(score.player1(), score.player2())) {
                int[] row = stats.get(player);

                if (row == null) {
                    continue;
                }

                row[0]++;
                row[player.equals(score.winner()) ? 1 : 2]++;
                row[3] += score.gamesWonBy(player);
                row[4] += score.gamesLostBy(player);
            }
        }

        List<Row> rows = new ArrayList<>();
        stats.forEach((player, row) -> rows.add(new Row(player, row[0], row[1], row[2], row[3], row[4],
                row[1] * pointsPerWin + adjustments.getOrDefault(player, 0))));

        rows.sort(Comparator.comparingInt(Row::points).reversed()
                .thenComparing(Comparator.comparingInt(Row::gameDifference).reversed()));

        return breakTies(rows, decided, lastTieBreak);
    }

    /**
     * Final order of a tournament with a bracket.
     *
     * @param table         standings table of the whole tournament
     * @param bracketSeries series of the bracket (upper, lower and grand final)
     * @return tournament player ids from first to last
     */
    public static List<UUID> finalOrder(List<Row> table, Collection<SeriesScore> bracketSeries) {
        Map<UUID, Integer> level = new HashMap<>();
        table.forEach(row -> level.put(row.participant(), NOT_IN_BRACKET));

        for (SeriesScore score : bracketSeries) {
            for (UUID player : new UUID[]{score.player1(), score.player2()}) {
                if (player != null && level.getOrDefault(player, NOT_IN_BRACKET) == NOT_IN_BRACKET) {
                    level.put(player, ALIVE);
                }
            }
        }

        for (SeriesScore score : bracketSeries) {
            if (!score.decided()) {
                continue;
            }

            if (score.stage() == MatchStage.GRAND_FINAL) {
                level.put(score.winner(), CHAMPION);
                level.put(score.loser(), FINALIST);
            } else if (score.stage() == MatchStage.LOWER) {
                level.merge(score.loser(), KNOCKED_OUT + score.round(), (current, knockedOut) ->
                        current >= FINALIST ? current : knockedOut);
            }
        }

        List<UUID> order = new ArrayList<>(table.stream().map(Row::participant).toList());
        // Stable sort: same level keeps the table order.
        order.sort(Comparator.comparingInt((UUID player) -> level.getOrDefault(player, NOT_IN_BRACKET)).reversed());

        return order;
    }

    /** Re-orders every block of rows tied on points and game difference. */
    private static List<Row> breakTies(List<Row> rows, List<SeriesScore> decided, Comparator<UUID> lastTieBreak) {
        List<Row> result = new ArrayList<>();
        int start = 0;

        while (start < rows.size()) {
            int end = start + 1;

            while (end < rows.size() && sameKey(rows.get(start), rows.get(end))) {
                end++;
            }

            List<Row> block = new ArrayList<>(rows.subList(start, end));

            if (block.size() > 1) {
                Set<UUID> tied = block.stream().map(Row::participant).collect(Collectors.toSet());
                Map<UUID, Integer> winsAmongTied = new HashMap<>();

                for (SeriesScore score : decided) {
                    if (tied.contains(score.player1()) && tied.contains(score.player2())) {
                        winsAmongTied.merge(score.winner(), 1, Integer::sum);
                    }
                }

                block.sort(Comparator.comparingInt((Row row) -> winsAmongTied.getOrDefault(row.participant(), 0)).reversed()
                        .thenComparing(Row::participant, lastTieBreak));
            }

            result.addAll(block);
            start = end;
        }

        return result;
    }

    private static boolean sameKey(Row first, Row second) {
        return first.points() == second.points() && first.gameDifference() == second.gameDifference();
    }
}
