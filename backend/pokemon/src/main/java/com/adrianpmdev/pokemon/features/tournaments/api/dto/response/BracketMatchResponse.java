package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for bracket match data.
 *
 * @author adrianpmdev
 */
public record BracketMatchResponse(
        UUID id,
        UUID tournamentId,
        UUID bracketStageId,
        Short matchNumber,
        UUID participant1Id,
        UUID participant2Id,
        UUID winnerId,
        String replayUrl,
        String status,
        Instant scheduledAt,
        Instant completedAt,
        Instant createdAt
) {}