package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * New data of a player inside a tournament.
 *
 * @param group            A, B or null (league, or the player who skips the groups)
 * @param pointsAdjustment points added to (or taken from) the series wins
 *
 * @author adrianpmdev
 */
public record TournamentPlayerUpdateRequest(
        @Pattern(regexp = "[AB]") String group,
        @Min(-999) @Max(999) int pointsAdjustment
) {
}
