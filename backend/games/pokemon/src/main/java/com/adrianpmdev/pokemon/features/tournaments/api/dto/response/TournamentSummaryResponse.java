package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import com.adrianpmdev.pokemon.shared.dto.RefResponse;

import java.util.UUID;

/**
 * A tournament in a list.
 *
 * @param id       tournament id
 * @param game     game it is played in
 * @param position order inside the randomlocke
 * @param name     tournament name
 * @param format   groups_bracket or round_robin
 * @param status   draft, in_progress or finished
 * @param champion winner of a finished tournament, or null
 *
 * @author adrianpmdev
 */
public record TournamentSummaryResponse(
        UUID id,
        RefResponse game,
        int position,
        String name,
        String format,
        String status,
        RefResponse champion
) {
}
