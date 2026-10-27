package com.adrianpmdev.pokemon.features.lockes.api.dto.response;

import com.adrianpmdev.pokemon.shared.dto.RefResponse;

import java.util.List;
import java.util.UUID;

/**
 * A randomlocke in a list.
 *
 * @param id              locke id
 * @param name            locke name
 * @param description     description
 * @param games           games its tournaments were played in, in play order
 * @param tournamentCount number of tournaments
 * @param playerCount     players in the roster
 *
 * @author adrianpmdev
 */
public record LockeSummaryResponse(
        UUID id,
        String name,
        String description,
        List<RefResponse> games,
        int tournamentCount,
        int playerCount
) {
}
