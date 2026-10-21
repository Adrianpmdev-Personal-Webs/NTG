package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for updating group matches.
 *
 * @author adrianpmdev
 */
public record GroupMatchUpdateRequest(
        UUID groupId,
        UUID participant1Id,
        UUID participant2Id,
        UUID winnerId,
        String replayUrl,
        String status,
        Instant scheduledAt,
        Instant completedAt
) {}