package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Data of a reward.
 *
 * @param name     reward name
 * @param chosenBy tournament player who picked it, or null
 *
 * @author adrianpmdev
 */
public record RewardRequest(
        @NotBlank @Size(max = 150) String name,
        UUID chosenBy
) {
}
