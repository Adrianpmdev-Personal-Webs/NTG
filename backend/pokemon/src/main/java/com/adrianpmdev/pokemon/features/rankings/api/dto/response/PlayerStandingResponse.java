package com.adrianpmdev.pokemon.features.rankings.api.dto.response;

import java.util.UUID;

/**
 * One player in the standings of a randomlocke or in the global ranking.
 *
 * @param playerId    player id
 * @param name        player name
 * @param position    1-based position
 * @param points      points over the counted tournaments
 * @param wins        series won
 * @param losses      series lost
 * @param tournaments tournaments played
 * @param titles      finished tournaments won
 * @param lockes      randomlockes played
 *
 * @author adrianpmdev
 */
public record PlayerStandingResponse(
        UUID playerId,
        String name,
        int position,
        int points,
        int wins,
        int losses,
        int tournaments,
        int titles,
        int lockes
) {
}
