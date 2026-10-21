package com.adrianpmdev.pokemon.features.games.api.dto.request;

/**
 * Request DTO for updating games.
 *
 * @author adrianpmdev
 */
public record GameUpdateRequest(
        String name,
        String description,
        String imageUrl
) {}