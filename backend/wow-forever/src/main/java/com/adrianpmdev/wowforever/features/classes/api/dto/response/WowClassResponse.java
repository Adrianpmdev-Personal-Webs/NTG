package com.adrianpmdev.wowforever.features.classes.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for class data.
 *
 * @author adrianpmdev
 */
public record WowClassResponse(
        UUID id,
        String slug,
        String name,
        String icon,
        int sortOrder,
        Instant updatedAt
) {}
