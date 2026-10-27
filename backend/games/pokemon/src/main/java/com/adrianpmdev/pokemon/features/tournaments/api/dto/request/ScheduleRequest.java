package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

/**
 * Options of the calendar generation.
 *
 * @param randomGroups draw the two groups again (groups_bracket only). When false the groups set by the
 *                     admin are used
 *
 * @author adrianpmdev
 */
public record ScheduleRequest(boolean randomGroups) {
}
