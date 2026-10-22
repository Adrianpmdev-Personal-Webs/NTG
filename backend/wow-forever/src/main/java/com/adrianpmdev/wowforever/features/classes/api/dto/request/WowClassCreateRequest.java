package com.adrianpmdev.wowforever.features.classes.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a class. The slug is the stable key and cannot change later.
 *
 * @author adrianpmdev
 */
public record WowClassCreateRequest(
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]*$", message = "Slug must be lowercase letters, digits or '-'")
        String slug,

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
