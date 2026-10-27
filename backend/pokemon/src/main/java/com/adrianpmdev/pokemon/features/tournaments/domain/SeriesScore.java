package com.adrianpmdev.pokemon.features.tournaments.domain;

import java.util.UUID;

/**
 * Score of one best-of-3 series, counted from its games. Pure value used by the tournament rules.
 *
 * @param matchId series id
 * @param stage   stage of the series
 * @param round   round inside the stage (1-based)
 * @param slot    position inside the round (1-based)
 * @param player1 tournament player on side 1, or null
 * @param player2 tournament player on side 2, or null
 * @param wins1   games won by side 1
 * @param wins2   games won by side 2
 *
 * @author adrianpmdev
 */
public record SeriesScore(
        UUID matchId,
        MatchStage stage,
        int round,
        int slot,
        UUID player1,
        UUID player2,
        int wins1,
        int wins2
) {

    /** Games a player needs to win a series. */
    public static final int WINS_NEEDED = 2;

    /** Most games a series can have. */
    public static final int MAX_GAMES = 2 * WINS_NEEDED - 1;

    /**
     * Whether somebody already won the series.
     *
     * @return true once a side has two wins
     */
    public boolean decided() {
        return player1 != null && player2 != null && (wins1 >= WINS_NEEDED || wins2 >= WINS_NEEDED);
    }

    /**
     * Winner of the series.
     *
     * @return tournament player id, or null while undecided
     */
    public UUID winner() {
        if (!decided()) {
            return null;
        }

        return wins1 >= WINS_NEEDED ? player1 : player2;
    }

    /**
     * Loser of the series.
     *
     * @return tournament player id, or null while undecided
     */
    public UUID loser() {
        if (!decided()) {
            return null;
        }

        return wins1 >= WINS_NEEDED ? player2 : player1;
    }

    /**
     * Whether the player plays this series.
     *
     * @param player tournament player id
     * @return true when the player is on one of the sides
     */
    public boolean involves(UUID player) {
        return player != null && (player.equals(player1) || player.equals(player2));
    }

    /**
     * Games won by one of the sides.
     *
     * @param player tournament player id on one of the sides
     * @return games won by that player
     */
    public int gamesWonBy(UUID player) {
        return player.equals(player1) ? wins1 : wins2;
    }

    /**
     * Games lost by one of the sides.
     *
     * @param player tournament player id on one of the sides
     * @return games lost by that player
     */
    public int gamesLostBy(UUID player) {
        return player.equals(player1) ? wins2 : wins1;
    }
}
