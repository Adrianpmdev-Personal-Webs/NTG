package com.adrianpmdev.pokemon.features.rewards.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating rewards.
 *
 * @author adrianpmdev
 */
public record RewardCreateRequest(
        @NotNull
        UUID tournamentId,

        @NotNull
        Short position,

        @NotBlank
        @Size(max = 100)
        String rewardName,

        Short rewardValue,

        @Size(max = 500)
        String imageUrl
) {}