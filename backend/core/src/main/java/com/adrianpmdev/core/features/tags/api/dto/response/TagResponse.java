package com.adrianpmdev.core.features.tags.api.dto.response;

import java.time.Instant;

/**
 * Response DTO with tag data.
 *
 * @param key         tag key
 * @param name        display name
 * @param description description
 * @param createdAt   creation instant
 *
 * @author adrianpmdev
 */
public record TagResponse(
    String key,
    String name,
    String description,
    Instant createdAt
) {}