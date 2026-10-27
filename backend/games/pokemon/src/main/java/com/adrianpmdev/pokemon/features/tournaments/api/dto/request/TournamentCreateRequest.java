package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Data of a new tournament. It starts in draft, with the roster of its randomlocke.
 *
 * @param lockeId       randomlocke
 * @param gameId        game it is played in
 * @param name          tournament name
 * @param description   optional description
 * @param format        groups_bracket or round_robin
 * @param pointsPerWin  points per series won
 * @param teamsRevealAt moment the team pictures become visible to everybody, or null
 *
 * @author adrianpmdev
 */
public record TournamentCreateRequest(
        @NotNull UUID lockeId,
        @NotNull UUID gameId,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotNull @Pattern(regexp = "groups_bracket|round_robin") String format,
        @Min(0) @Max(100) int pointsPerWin,
        Instant teamsRevealAt
) {
}
