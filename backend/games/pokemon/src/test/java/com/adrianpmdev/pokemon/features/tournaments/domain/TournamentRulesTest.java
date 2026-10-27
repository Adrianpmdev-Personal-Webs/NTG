package com.adrianpmdev.pokemon.features.tournaments.domain;

import com.adrianpmdev.pokemon.features.tournaments.domain.DoubleEliminationBracket.Place;
import com.adrianpmdev.pokemon.features.tournaments.domain.DoubleEliminationBracket.Resolved;
import com.adrianpmdev.pokemon.features.tournaments.domain.DoubleEliminationBracket.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the pure tournament rules: calendar, bracket structure and resolution, standings.
 *
 * @author adrianpmdev
 */
class TournamentRulesTest {

    private static List<UUID> players(int count) {
        return IntStream.range(0, count).mapToObj(i -> new UUID(0, i + 1)).toList();
    }

    private static Place upper(int round, int slot) {
        return new Place(MatchStage.UPPER, round, slot);
    }

    private static Place lower(int round, int slot) {
        return new Place(MatchStage.LOWER, round, slot);
    }

    private static final Place GRAND_FINAL = new Place(MatchStage.GRAND_FINAL, 1, 1);

    private static SeriesScore score(Place place, UUID p1, UUID p2, int wins1, int wins2) {
        return new SeriesScore(UUID.randomUUID(), place.stage(), place.round(), place.slot(), p1, p2, wins1, wins2);
    }

    // ─── Round robin ─────────────────────────────────────────────────────────────────────────────

    @Test
    void roundRobinPairsEverybodyOnceAndNobodyTwicePerRound() {
        for (int count = 2; count <= 9; count++) {
            List<UUID> players = players(count);
            List<RoundRobin.Pairing> pairings = RoundRobin.schedule(players);

            assertEquals(count * (count - 1) / 2, pairings.size(), "pairings for " + count);

            Set<Set<UUID>> pairs = new HashSet<>();
            pairings.forEach(p -> pairs.add(Set.of(p.player1(), p.player2())));
            assertEquals(pairings.size(), pairs.size(), "no repeated pair for " + count);

            Map<Integer, List<UUID>> byRound = new HashMap<>();
            pairings.forEach(p -> byRound.computeIfAbsent(p.round(), r -> new ArrayList<>()).addAll(List.of(p.player1(), p.player2())));
            byRound.values().forEach(list -> assertEquals(list.size(), new HashSet<>(list).size(), "nobody twice in a round"));
        }
    }

    // ─── Bracket structure ───────────────────────────────────────────────────────────────────────

    @Test
    void bracketHasTheExpectedNumberOfSeries() {
        // Double elimination without reset: 2P - 2 series.
        for (int size : new int[]{4, 8, 16, 32}) {
            assertEquals(2 * size - 2, DoubleEliminationBracket.ofSize(size).places().size(), "size " + size);
        }
    }

    @Test
    void bracketSizeFitsTheFirstRound() {
        assertEquals(4, DoubleEliminationBracket.sizeFor(1));
        assertEquals(4, DoubleEliminationBracket.sizeFor(2));
        assertEquals(8, DoubleEliminationBracket.sizeFor(3));
        assertEquals(8, DoubleEliminationBracket.sizeFor(4));
        assertEquals(16, DoubleEliminationBracket.sizeFor(5));
    }

    @Test
    void fourPlayerBracketPlaysThroughToTheGrandFinal() {
        List<UUID> p = players(4);
        DoubleEliminationBracket bracket = DoubleEliminationBracket.ofSize(4);
        Map<Place, SeriesScore> scores = new HashMap<>();
        scores.put(upper(1, 1), score(upper(1, 1), p.get(0), p.get(1), 2, 0));
        scores.put(upper(1, 2), score(upper(1, 2), p.get(2), p.get(3), 1, 2));

        Map<Place, Resolved> resolved = bracket.resolve(scores);
        assertEquals(State.READY, resolved.get(upper(2, 1)).state());
        assertEquals(p.get(0), resolved.get(upper(2, 1)).side1().player());
        assertEquals(p.get(3), resolved.get(upper(2, 1)).side2().player());
        assertEquals(State.READY, resolved.get(lower(1, 1)).state());
        assertEquals(p.get(1), resolved.get(lower(1, 1)).side1().player());
        assertEquals(p.get(2), resolved.get(lower(1, 1)).side2().player());
        assertEquals(State.PENDING, resolved.get(GRAND_FINAL).state());

        scores.put(upper(2, 1), score(upper(2, 1), p.get(0), p.get(3), 2, 1));
        scores.put(lower(1, 1), score(lower(1, 1), p.get(1), p.get(2), 0, 2));
        resolved = bracket.resolve(scores);
        // Lower round 2: winner of lower 1 against the loser of the upper final.
        assertEquals(p.get(2), resolved.get(lower(2, 1)).side1().player());
        assertEquals(p.get(3), resolved.get(lower(2, 1)).side2().player());

        scores.put(lower(2, 1), score(lower(2, 1), p.get(2), p.get(3), 2, 0));
        resolved = bracket.resolve(scores);
        assertEquals(p.get(0), resolved.get(GRAND_FINAL).side1().player());
        assertEquals(p.get(2), resolved.get(GRAND_FINAL).side2().player());
        assertEquals(State.READY, resolved.get(GRAND_FINAL).state());
    }

    @Test
    void sixPlayersFromGroupsGetByesInAnEightBracket() {
        List<UUID> a = players(3);
        List<UUID> b = IntStream.range(10, 13).mapToObj(i -> new UUID(0, i)).toList();
        List<UUID[]> firstRound = GroupsToBracket.firstRound(a, b, List.of());

        // A1-B3, A2-B2, A3-B1.
        assertEquals(a.get(0), firstRound.get(0)[0]);
        assertEquals(b.get(2), firstRound.get(0)[1]);
        assertEquals(a.get(2), firstRound.get(2)[0]);
        assertEquals(b.get(0), firstRound.get(2)[1]);

        int size = DoubleEliminationBracket.sizeFor(firstRound.size());
        assertEquals(8, size);
        DoubleEliminationBracket bracket = DoubleEliminationBracket.ofSize(size);

        Map<Place, SeriesScore> scores = new HashMap<>();
        for (int slot = 1; slot <= bracket.firstRoundSeries(); slot++) {
            UUID[] pair = slot <= firstRound.size() ? firstRound.get(slot - 1) : new UUID[]{null, null};
            scores.put(upper(1, slot), score(upper(1, slot), pair[0], pair[1], 0, 0));
        }

        Map<Place, Resolved> resolved = bracket.resolve(scores);
        assertEquals(State.VOID, resolved.get(upper(1, 4)).state());
        assertEquals(State.PENDING, resolved.get(upper(2, 2)).state());
        // Lower series fed by the void series: one side can never have a player.
        assertTrue(resolved.get(lower(1, 2)).side2().empty());

        // Play the first round: side 1 always wins.
        for (int slot = 1; slot <= 3; slot++) {
            UUID[] pair = firstRound.get(slot - 1);
            scores.put(upper(1, slot), score(upper(1, slot), pair[0], pair[1], 2, 0));
        }

        resolved = bracket.resolve(scores);
        assertEquals(State.BYE, resolved.get(upper(2, 2)).state(), "winner of series 3 goes through");
        assertEquals(a.get(2), resolved.get(upper(2, 2)).winner().player());
        assertEquals(State.BYE, resolved.get(lower(1, 2)).state(), "loser of series 3 goes through on the lower side");
        assertEquals(b.get(0), resolved.get(lower(1, 2)).winner().player());
        assertEquals(State.READY, resolved.get(lower(1, 1)).state());
        // Upper final: winner of series 1/2 against the player with the bye.
        assertEquals(State.READY, resolved.get(upper(2, 1)).state());
        assertEquals(State.PENDING, resolved.get(upper(3, 1)).state());
    }

    @Test
    void changedEarlierResultInvalidatesLaterGames() {
        List<UUID> p = players(4);
        DoubleEliminationBracket bracket = DoubleEliminationBracket.ofSize(4);
        Map<Place, SeriesScore> scores = new HashMap<>();
        scores.put(upper(1, 1), score(upper(1, 1), p.get(0), p.get(1), 2, 0));
        scores.put(upper(1, 2), score(upper(1, 2), p.get(2), p.get(3), 2, 0));
        scores.put(upper(2, 1), score(upper(2, 1), p.get(0), p.get(2), 2, 0));

        // The admin corrects series 1: player 2 won it. The stored upper final no longer matches.
        scores.put(upper(1, 1), score(upper(1, 1), p.get(0), p.get(1), 0, 2));
        Resolved upperFinal = bracket.resolve(scores).get(upper(2, 1));
        assertEquals(p.get(1), upperFinal.side1().player());
        assertEquals(State.READY, upperFinal.state());
    }

    // ─── Standings ───────────────────────────────────────────────────────────────────────────────

    @Test
    void tableOrdersByPointsThenGameDifferenceThenHeadToHead() {
        List<UUID> p = players(3);
        Place league = new Place(MatchStage.LEAGUE, 1, 1);
        List<SeriesScore> series = List.of(
                score(league, p.get(0), p.get(1), 1, 2),
                score(league, p.get(1), p.get(2), 0, 2),
                score(league, p.get(2), p.get(0), 1, 2));

        // Everybody has one win (3 points); game difference decides: p2 +1, p0 0, p1 -1.
        List<Standings.Row> table = Standings.table(p, series, 3, Map.of(), Comparator.naturalOrder());
        assertEquals(List.of(p.get(2), p.get(0), p.get(1)), table.stream().map(Standings.Row::participant).toList());
        assertEquals(3, table.getFirst().points());

        // An adjustment goes before game difference.
        table = Standings.table(p, series, 3, Map.of(p.get(1), 1), Comparator.naturalOrder());
        assertEquals(p.get(1), table.getFirst().participant());
        assertEquals(4, table.getFirst().points());
    }

    @Test
    void headToHeadDecidesATwoWayTie() {
        List<UUID> p = players(4);
        Place league = new Place(MatchStage.LEAGUE, 1, 1);
        List<SeriesScore> series = List.of(
                score(league, p.get(0), p.get(1), 1, 2), // p1 beats p0
                score(league, p.get(0), p.get(2), 2, 0),
                score(league, p.get(1), p.get(2), 2, 0),
                score(league, p.get(0), p.get(3), 2, 1),
                score(league, p.get(1), p.get(3), 1, 2));

        List<UUID> order = Standings.table(p, series, 1, Map.of(), Comparator.naturalOrder())
                .stream().map(Standings.Row::participant).toList();
        // p0 and p1: 2 wins and +2 each → direct series: p1 first, although p0 sorts first by id.
        assertEquals(List.of(p.get(1), p.get(0), p.get(3), p.get(2)), order);
    }

    @Test
    void finalOrderFollowsTheOldSixPlayerBracket() {
        // Tournament "Torneo 1 - LvL 19" of the imported data: the final order was champion, finalist,
        // then the losers of lower final, lower 3, lower 2 and lower 1.
        List<UUID> p = players(6);
        UUID pr3y = p.get(0), monty = p.get(1), barroso = p.get(2), ethan = p.get(3), aaron = p.get(4), alberto = p.get(5);
        List<SeriesScore> bracket = List.of(
                score(upper(1, 1), monty, ethan, 2, 1),
                score(upper(1, 2), barroso, aaron, 2, 1),
                score(lower(1, 1), alberto, pr3y, 0, 2),
                score(lower(2, 1), pr3y, aaron, 2, 1),
                score(upper(2, 1), monty, barroso, 2, 1),
                score(lower(3, 1), pr3y, ethan, 2, 0),
                score(lower(4, 1), pr3y, barroso, 2, 1),
                score(GRAND_FINAL, monty, pr3y, 0, 2));

        List<Standings.Row> table = Standings.table(p, bracket, 1, Map.of(), Comparator.naturalOrder());
        assertEquals(List.of(pr3y, monty, barroso, ethan, aaron, alberto), Standings.finalOrder(table, bracket));
    }

    @Test
    void undecidedSeriesDoNotCount() {
        List<UUID> p = players(2);
        SeriesScore halfPlayed = score(new Place(MatchStage.LEAGUE, 1, 1), p.get(0), p.get(1), 1, 0);
        Standings.Row first = Standings.table(p, List.of(halfPlayed), 1, Map.of(), Comparator.naturalOrder()).getFirst();
        assertEquals(0, first.played());
        assertNull(halfPlayed.winner());
    }
}
