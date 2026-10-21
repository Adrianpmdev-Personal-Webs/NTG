package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for bracket stage data.
 *
 * @author adrianpmdev
 */
public record BracketStageResponse(
        UUID id,
        UUID tournamentId,
        String stageName,
        Short stageOrder,
        String status,
        Instant createdAt
) {}