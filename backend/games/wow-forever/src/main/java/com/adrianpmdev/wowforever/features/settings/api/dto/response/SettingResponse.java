package com.adrianpmdev.wowforever.features.settings.api.dto.response;

import java.time.Instant;

/**
 * Response DTO for setting data.
 *
 * @author adrianpmdev
 */
public record SettingResponse(
        String key,
        String value,
        String description,
        Instant updatedAt
) {}
