package com.adrianpmdev.wowforever.features.settings.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for changing the value of a setting.
 *
 * @author adrianpmdev
 */
public record SettingUpdateRequest(
        @NotBlank
        @Size(max = 255)
        String value
) {}
