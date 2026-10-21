package com.adrianpmdev.pokemon.features.games.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating games.
 *
 * @author adrianpmdev
 */
public record GameCreateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotBlank
        @Size(max = 500)
        String imageUrl
) {}