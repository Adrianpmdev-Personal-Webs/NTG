package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.time.Instant;

/**
 * One game of a series.
 *
 * @param number     1, 2 or 3
 * @param winnerSide 1 or 2
 * @param replayUrl  replay link, or null
 * @param playedAt   when it was recorded
 *
 * @author adrianpmdev
 */
public record MatchGameResponse(int number, int winnerSide, String replayUrl, Instant playedAt) {
}
