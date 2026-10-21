package com.adrianpmdev.pokemon.features.rewards.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for creating user rewards.
 *
 * @author adrianpmdev
 */
public record UserRewardCreateRequest(
        @NotNull
        UUID userId,

        @NotNull
        UUID rewardId,

        @NotNull
        UUID tournamentId
) {}