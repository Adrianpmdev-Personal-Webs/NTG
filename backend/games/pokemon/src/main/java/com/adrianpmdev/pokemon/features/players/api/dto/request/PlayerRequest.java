package com.adrianpmdev.pokemon.features.players.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Data of a player, to create it or to replace it.
 *
 * @param name   display name (unique)
 * @param userId platform account to link, or null
 *
 * @author adrianpmdev
 */
public record PlayerRequest(
        @NotBlank @Size(max = 50) String name,
        UUID userId
) {
}
