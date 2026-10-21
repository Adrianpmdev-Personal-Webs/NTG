package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating bracket stages.
 *
 * @author adrianpmdev
 */
public record BracketStageCreateRequest(
        @NotNull
        UUID tournamentId,

        @NotBlank
        @Size(max = 50)
        String stageName,

        @NotNull
        Short stageOrder,

        @NotBlank
        @Size(max = 20)
        String status
) {}