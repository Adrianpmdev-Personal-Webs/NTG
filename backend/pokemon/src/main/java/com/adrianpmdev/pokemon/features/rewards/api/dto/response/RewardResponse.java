package com.adrianpmdev.pokemon.features.rewards.api.dto.response;

import java.util.UUID;

/**
 * Response DTO for reward data.
 *
 * @author adrianpmdev
 */
public record RewardResponse(
        UUID id,
        UUID tournamentId,
        Short position,
        String rewardName,
        Short rewardValue,
        String imageUrl
) {}