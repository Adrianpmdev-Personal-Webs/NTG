package com.adrianpmdev.wowforever.features.classes.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a class.
 *
 * @author adrianpmdev
 */
public record WowClassUpdateRequest(
        @NotBlank
        @Size(max = 60)
        String name,

        @NotBlank
        @Size(max = 100)
        String icon,

        @NotNull
        @Min(0)
        Integer sortOrder
) {}
