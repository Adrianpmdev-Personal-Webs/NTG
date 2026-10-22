package com.adrianpmdev.wowforever.features.races.api.dto.request;

import com.adrianpmdev.wowforever.features.races.infrastructure.persistence.entity.Faction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a race. The slug is the stable key and cannot change later.
 *
 * @author adrianpmdev
 */
public record RaceCreateRequest(
        @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]*$", message = "Slug must be lowercase letters, digits or '-'")
        String slug,

        @NotBlank
        @Size(max = 60)
        String name,

        @NotNull
        Faction faction,

        @NotBlank
        @Size(max = 100)
        String icon,

        @NotNull
        @Min(0)
        Integer sortOrder,

        @NotNull
        List<UUID> classIds
) {}
