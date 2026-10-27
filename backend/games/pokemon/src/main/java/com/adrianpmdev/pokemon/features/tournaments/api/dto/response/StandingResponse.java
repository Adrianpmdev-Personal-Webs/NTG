package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.util.UUID;

/**
 * One row of a standings table.
 *
 * @param tournamentPlayerId tournament player id
 * @param position           1-based position
 * @param played             series played
 * @param wins               series won
 * @param losses             series lost
 * @param gamesWon           games won
 * @param gamesLost          games lost
 * @param points             points
 *
 * @author adrianpmdev
 */
public record StandingResponse(
        UUID tournamentPlayerId,
        int position,
        int played,
        int wins,
        int losses,
        int gamesWon,
        int gamesLost,
        int points
) {
}
