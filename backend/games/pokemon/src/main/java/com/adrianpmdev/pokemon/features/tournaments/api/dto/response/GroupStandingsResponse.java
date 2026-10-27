package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.util.List;

/**
 * Table of one group.
 *
 * @param group     group name (A or B)
 * @param standings rows in order
 *
 * @author adrianpmdev
 */
public record GroupStandingsResponse(String group, List<StandingResponse> standings) {
}
