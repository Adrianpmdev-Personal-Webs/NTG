package com.adrianpmdev.pokemon.features.rewards.api.dto.request;

import java.util.UUID;

/**
 * Request DTO for updating user rewards.
 *
 * @author adrianpmdev
 */
public record UserRewardUpdateRequest(
        UUID userId,
        UUID rewardId,
        UUID tournamentId
) {}