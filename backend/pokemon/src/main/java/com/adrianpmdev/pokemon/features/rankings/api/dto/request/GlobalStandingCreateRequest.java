package com.adrianpmdev.pokemon.features.rankings.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for creating global standings.
 *
 * @author adrianpmdev
 */
public record GlobalStandingCreateRequest(
        @NotNull
        UUID userId,

        @NotNull
        @Min(0)
        Short totalTournaments,

        @NotNull
        @Min(0)
        Short tournamentsWon,

        @NotNull
        @Min(0)
        Integer totalPoints,

        @Min(1)
        Short rankingPosition
) {}