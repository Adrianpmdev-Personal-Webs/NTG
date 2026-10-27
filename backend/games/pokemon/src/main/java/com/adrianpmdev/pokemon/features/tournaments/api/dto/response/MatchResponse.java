package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * A best-of-3 series.
 *
 * @param id        series id
 * @param stage     group, league, upper, lower or grand_final
 * @param round     round inside the stage
 * @param slot      position inside the round
 * @param player1Id tournament player on side 1, or null
 * @param player2Id tournament player on side 2, or null
 * @param score1    games won by side 1
 * @param score2    games won by side 2
 * @param winnerId  winner, or null while undecided
 * @param status    pending, ready, finished, bye or void
 * @param games     games of the series
 *
 * @author adrianpmdev
 */
public record MatchResponse(
        UUID id,
        String stage,
        int round,
        int slot,
        UUID player1Id,
        UUID player2Id,
        int score1,
        int score2,
        UUID winnerId,
        String status,
        List<MatchGameResponse> games
) {
}
