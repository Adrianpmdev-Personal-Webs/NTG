package com.adrianpmdev.pokemon.features.rewards.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user reward data.
 *
 * @author adrianpmdev
 */
public record UserRewardResponse(
        UUID id,
        UUID userId,
        UUID rewardId,
        UUID tournamentId,
        Instant claimedAt
) {}