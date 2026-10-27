package com.adrianpmdev.pokemon.features.games.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data of a game, to create it or to replace it.
 *
 * @param name        game name
 * @param description optional description
 * @param imageUrl    cover picture (http or https URL)
 *
 * @author adrianpmdev
 */
public record GameRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "must be an http(s) URL") String imageUrl
) {
}
