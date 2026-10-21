package com.adrianpmdev.pokemon.features.lockes.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for locke data.
 *
 * @author adrianpmdev
 */
public record LockeResponse(
        UUID id,
        UUID gameId,
        String name,
        String description,
        Object rules,
        Instant createdAt
) {}