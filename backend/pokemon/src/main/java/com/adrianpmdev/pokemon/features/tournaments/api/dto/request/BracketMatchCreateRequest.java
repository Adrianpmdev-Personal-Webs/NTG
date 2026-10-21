package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for creating bracket matches.
 *
 * @author adrianpmdev
 */
public record BracketMatchCreateRequest(
        @NotNull
        UUID tournamentId,

        @NotNull
        UUID bracketStageId,

        @NotNull
        Short matchNumber,

        UUID participant1Id,

        UUID participant2Id,

        UUID winnerId,

        @Size(max = 500)
        String replayUrl,

        @NotBlank
        @Size(max = 20)
        String status,

        Instant scheduledAt,

        Instant completedAt
) {}