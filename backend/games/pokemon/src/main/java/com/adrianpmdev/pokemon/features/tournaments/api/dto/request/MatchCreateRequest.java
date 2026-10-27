package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * A series added by hand (for corrections; calendars and brackets are normally generated).
 *
 * @param stage     group, league, upper, lower or grand_final
 * @param round     round inside the stage
 * @param slot      position inside the round
 * @param player1Id tournament player on side 1
 * @param player2Id tournament player on side 2
 *
 * @author adrianpmdev
 */
public record MatchCreateRequest(
        @NotNull @Pattern(regexp = "group|league|upper|lower|grand_final") String stage,
        @Min(1) @Max(99) int round,
        @Min(1) @Max(99) int slot,
        @NotNull UUID player1Id,
        @NotNull UUID player2Id
) {
}
