package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for updating bracket matches.
 *
 * @author adrianpmdev
 */
public record BracketMatchUpdateRequest(
        UUID tournamentId,
        UUID bracketStageId,
        Short matchNumber,
        UUID participant1Id,
        UUID participant2Id,
        UUID winnerId,
        String replayUrl,
        String status,
        Instant scheduledAt,
        Instant completedAt
) {}