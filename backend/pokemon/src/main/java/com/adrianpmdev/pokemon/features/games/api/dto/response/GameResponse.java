package com.adrianpmdev.pokemon.features.games.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for game data.
 *
 * @author adrianpmdev
 */
public record GameResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        Instant createdAt
) {}