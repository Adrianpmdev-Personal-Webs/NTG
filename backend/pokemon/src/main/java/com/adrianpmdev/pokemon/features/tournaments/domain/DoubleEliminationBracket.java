package com.adrianpmdev.pokemon.features.tournaments.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Structure and resolution of a double-elimination bracket of a power-of-two size.
 *
 * <p>The structure says where each series takes its players from: the seeds in the first upper round,
 * and the winner or loser of an earlier series everywhere else. For a size {@code P = 2^k}:
 * <ul>
 *     <li>Upper round {@code r} (1..k) has {@code P / 2^r} series; the winners of series {@code 2s-1} and
 *     {@code 2s} meet in series {@code s} of the next round.</li>
 *     <li>Lower round 1 pairs the losers of upper round 1.</li>
 *     <li>Even lower rounds {@code 2j} put the winner of lower series {@code s} against the loser of upper
 *     series {@code s} of round {@code j+1}.</li>
 *     <li>Odd lower rounds {@code 2j-1 (j >= 2)} pair the winners of the previous lower round.</li>
 *     <li>The grand final is the upper winner against the lower winner (one best-of-3, no reset).</li>
 * </ul>
 *
 * <p>Brackets usually have fewer players than slots. An empty seed is a side that will never have a
 * player, so a series with a player against an empty side is a bye (the player goes through and nobody
 * drops to the lower side), and a series with two empty sides is void. Resolution walks the structure
 * from the stored seeds and series scores and tells, for every series, who plays it and its state.
 *
 * @author adrianpmdev
 */
public final class DoubleEliminationBracket {

    /** Smallest bracket: two upper series, so there is a lower side and a grand final. */
    public static final int MIN_SIZE = 4;

    /** Largest bracket the app generates. */
    public static final int MAX_SIZE = 32;

    /**
     * Position of a series in the bracket.
     *
     * @param stage upper, lower or grand final
     * @param round round inside the stage (1-based)
     * @param slot  position inside the round (1-based)
     */
    public record Place(MatchStage stage, int round, int slot) {
    }

    /** Where one side of a series takes its player from. */
    public sealed interface Source permits Seed, WinnerOf, LoserOf {
    }

    /**
     * A stored seed of the first upper round.
     *
     * @param side 1 or 2
     */
    public record Seed(int side) implements Source {
    }

    /**
     * The winner of an earlier series.
     *
     * @param place earlier series
     */
    public record WinnerOf(Place place) implements Source {
    }

    /**
     * The loser of an earlier series.
     *
     * @param place earlier series
     */
    public record LoserOf(Place place) implements Source {
    }

    /** State of a series after resolution. */
    public enum State {
        /** Waiting for an earlier result. */
        PENDING,
        /** Both players known, not decided yet. */
        READY,
        /** Decided by its games. */
        FINISHED,
        /** One player against an empty side: the player goes through without playing. */
        BYE,
        /** Two empty sides: nobody plays it. */
        VOID
    }

    /**
     * One side of a resolved series.
     *
     * @param player tournament player id, or null
     * @param empty  true when no player will ever take this side; false with a null player means pending
     */
    public record Side(UUID player, boolean empty) {

        static final Side EMPTY = new Side(null, true);
        static final Side PENDING = new Side(null, false);

        static Side of(UUID player) {
            return player == null ? EMPTY : new Side(player, false);
        }

        /**
         * Whether a player holds the side.
         *
         * @return true when there is a player
         */
        public boolean known() {
            return player != null;
        }
    }

    /**
     * A resolved series.
     *
     * @param side1  first side
     * @param side2  second side
     * @param state  state of the series
     * @param winner who goes on (a player, empty or pending)
     * @param loser  who drops or leaves (a player, empty or pending)
     */
    public record Resolved(Side side1, Side side2, State state, Side winner, Side loser) {
    }

    private final int size;
    private final int upperRounds;
    private final Map<Place, Source[]> sources = new HashMap<>();
    private final List<Place> places = new ArrayList<>();

    private DoubleEliminationBracket(int size) {
        this.size = size;
        this.upperRounds = Integer.numberOfTrailingZeros(size);
        build();
    }

    /**
     * Bracket of a given size.
     *
     * @param size power of two between {@link #MIN_SIZE} and {@link #MAX_SIZE}
     * @return bracket structure
     */
    public static DoubleEliminationBracket ofSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE || Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException("Bracket size must be a power of two between 4 and 32: " + size);
        }

        return new DoubleEliminationBracket(size);
    }

    /**
     * Smallest bracket with at least the given number of first-round series.
     *
     * @param firstRoundEntries series needed in the first upper round
     * @return bracket size
     */
    public static int sizeFor(int firstRoundEntries) {
        int size = MIN_SIZE;

        while (size / 2 < firstRoundEntries) {
            size *= 2;
        }

        return size;
    }

    /**
     * Number of slots (players) of the bracket.
     *
     * @return bracket size
     */
    public int size() {
        return size;
    }

    /**
     * Every series of the bracket, in an order where each series comes after the ones it depends on.
     *
     * @return places of the bracket
     */
    public List<Place> places() {
        return List.copyOf(places);
    }

    /**
     * Number of series in the first upper round, which hold the seeds.
     *
     * @return series in upper round 1
     */
    public int firstRoundSeries() {
        return size / 2;
    }

    /**
     * Number of lower rounds.
     *
     * @return lower rounds
     */
    public int lowerRounds() {
        return 2 * (upperRounds - 1);
    }

    /**
     * Where the two sides of a series come from.
     *
     * @param place series
     * @return sources of side 1 and side 2
     */
    public Source[] sourcesOf(Place place) {
        Source[] found = sources.get(place);

        if (found == null) {
            throw new IllegalArgumentException("Not a place of this bracket: " + place);
        }

        return found.clone();
    }

    /**
     * Resolves every series of the bracket.
     *
     * @param scores stored series by place. For upper round 1 the stored players are the seeds; for the
     *               rest, games only count when the stored players are the ones the structure gives
     * @return resolved series by place
     */
    public Map<Place, Resolved> resolve(Map<Place, SeriesScore> scores) {
        Map<Place, Resolved> resolved = new HashMap<>();

        // places is in dependency order, so every source is already resolved when it is needed.
        for (Place place : places) {
            Source[] pair = sources.get(place);
            Side side1 = sideFrom(pair[0], place, scores, resolved);
            Side side2 = sideFrom(pair[1], place, scores, resolved);
            resolved.put(place, resolveSeries(side1, side2, scores.get(place)));
        }

        return resolved;
    }

    private Side sideFrom(Source source, Place place, Map<Place, SeriesScore> scores, Map<Place, Resolved> resolved) {
        return switch (source) {
            case Seed seed -> {
                SeriesScore stored = scores.get(place);
                yield stored == null ? Side.EMPTY : Side.of(seed.side() == 1 ? stored.player1() : stored.player2());
            }
            case WinnerOf winnerOf -> resolved.get(winnerOf.place()).winner();
            case LoserOf loserOf -> resolved.get(loserOf.place()).loser();
        };
    }

    private static Resolved resolveSeries(Side side1, Side side2, SeriesScore stored) {
        if (side1.known() && side2.known()) {
            int[] wins = winsFor(side1.player(), side2.player(), stored);

            if (wins[0] >= SeriesScore.WINS_NEEDED) {
                return new Resolved(side1, side2, State.FINISHED, side1, side2);
            }

            if (wins[1] >= SeriesScore.WINS_NEEDED) {
                return new Resolved(side1, side2, State.FINISHED, side2, side1);
            }

            return new Resolved(side1, side2, State.READY, Side.PENDING, Side.PENDING);
        }

        if (side1.empty() && side2.empty()) {
            return new Resolved(side1, side2, State.VOID, Side.EMPTY, Side.EMPTY);
        }

        if (side1.known() && side2.empty()) {
            return new Resolved(side1, side2, State.BYE, side1, Side.EMPTY);
        }

        if (side2.known() && side1.empty()) {
            return new Resolved(side1, side2, State.BYE, side2, Side.EMPTY);
        }

        // At least one side is still pending. If the other side is empty it will be a bye: no loser.
        Side loser = side1.empty() || side2.empty() ? Side.EMPTY : Side.PENDING;

        return new Resolved(side1, side2, State.PENDING, Side.PENDING, loser);
    }

    /** Games of the stored series, oriented to the resolved sides; nothing when the players differ. */
    private static int[] winsFor(UUID player1, UUID player2, SeriesScore stored) {
        if (stored == null) {
            return new int[]{0, 0};
        }

        if (player1.equals(stored.player1()) && player2.equals(stored.player2())) {
            return new int[]{stored.wins1(), stored.wins2()};
        }

        if (player1.equals(stored.player2()) && player2.equals(stored.player1())) {
            return new int[]{stored.wins2(), stored.wins1()};
        }

        return new int[]{0, 0};
    }

    private void build() {
        // Upper rounds.
        for (int round = 1; round <= upperRounds; round++) {
            int count = size >> round;

            for (int slot = 1; slot <= count; slot++) {
                Source[] pair = round == 1
                        ? new Source[]{new Seed(1), new Seed(2)}
                        : new Source[]{
                                new WinnerOf(new Place(MatchStage.UPPER, round - 1, 2 * slot - 1)),
                                new WinnerOf(new Place(MatchStage.UPPER, round - 1, 2 * slot))};
                add(new Place(MatchStage.UPPER, round, slot), pair);
            }

            // Lower rounds that only need this upper round: 2j-1 and 2j need upper rounds j and j+1.
            if (round >= 2) {
                addLowerRounds(round - 1);
            }
        }

        Place lastUpper = new Place(MatchStage.UPPER, upperRounds, 1);
        Place lastLower = new Place(MatchStage.LOWER, lowerRounds(), 1);
        add(new Place(MatchStage.GRAND_FINAL, 1, 1), new Source[]{new WinnerOf(lastUpper), new WinnerOf(lastLower)});
    }

    /** Adds lower rounds 2j-1 and 2j. */
    private void addLowerRounds(int j) {
        int count = size >> (j + 1);
        int oddRound = 2 * j - 1;
        int evenRound = 2 * j;

        for (int slot = 1; slot <= count; slot++) {
            Source[] pair = j == 1
                    ? new Source[]{
                            new LoserOf(new Place(MatchStage.UPPER, 1, 2 * slot - 1)),
                            new LoserOf(new Place(MatchStage.UPPER, 1, 2 * slot))}
                    : new Source[]{
                            new WinnerOf(new Place(MatchStage.LOWER, oddRound - 1, 2 * slot - 1)),
                            new WinnerOf(new Place(MatchStage.LOWER, oddRound - 1, 2 * slot))};
            add(new Place(MatchStage.LOWER, oddRound, slot), pair);
        }

        for (int slot = 1; slot <= count; slot++) {
            add(new Place(MatchStage.LOWER, evenRound, slot), new Source[]{
                    new WinnerOf(new Place(MatchStage.LOWER, oddRound, slot)),
                    new LoserOf(new Place(MatchStage.UPPER, j + 1, slot))});
        }
    }

    private void add(Place place, Source[] pair) {
        sources.put(place, pair);
        places.add(place);
    }
}
