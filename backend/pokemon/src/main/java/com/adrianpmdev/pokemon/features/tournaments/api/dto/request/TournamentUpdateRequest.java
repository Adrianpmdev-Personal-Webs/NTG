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
 * New data of a tournament. The format cannot change once created.
 *
 * @param gameId        game it is played in
 * @param name          tournament name
 * @param description   optional description
 * @param status        draft, in_progress or finished
 * @param pointsPerWin  points per series won
 * @param teamsRevealAt moment the team pictures become visible to everybody, or null
 *
 * @author adrianpmdev
 */
public record TournamentUpdateRequest(
        @NotNull UUID gameId,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotNull @Pattern(regexp = "draft|in_progress|finished") String status,
        @Min(0) @Max(100) int pointsPerWin,
        Instant teamsRevealAt
) {
}
