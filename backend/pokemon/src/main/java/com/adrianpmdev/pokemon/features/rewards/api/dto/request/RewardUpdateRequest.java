package com.adrianpmdev.pokemon.features.rewards.api.dto.request;

import java.util.UUID;

/**
 * Request DTO for updating rewards.
 *
 * @author adrianpmdev
 */
public record RewardUpdateRequest(
        UUID tournamentId,
        Short position,
        String rewardName,
        Short rewardValue,
        String imageUrl
) {}