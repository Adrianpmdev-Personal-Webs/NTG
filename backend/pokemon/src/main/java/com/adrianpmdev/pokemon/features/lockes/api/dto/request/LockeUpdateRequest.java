package com.adrianpmdev.pokemon.features.lockes.api.dto.request;

import java.util.UUID;

/**
 * Request DTO for updating lockes.
 *
 * @author adrianpmdev
 */
public record LockeUpdateRequest(
        UUID gameId,
        String name,
        String description,
        Object rules
) {}