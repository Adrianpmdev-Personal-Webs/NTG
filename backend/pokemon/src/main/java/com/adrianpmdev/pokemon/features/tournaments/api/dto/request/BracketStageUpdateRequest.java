package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import java.util.UUID;

/**
 * Request DTO for updating bracket stages.
 *
 * @author adrianpmdev
 */
public record BracketStageUpdateRequest(
        UUID tournamentId,
        String stageName,
        Short stageOrder,
        String status
) {}