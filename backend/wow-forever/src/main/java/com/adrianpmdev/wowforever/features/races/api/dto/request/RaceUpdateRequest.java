package com.adrianpmdev.wowforever.features.races.api.dto.request;

import com.adrianpmdev.wowforever.features.races.infrastructure.persistence.entity.Faction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating a race. classIds replaces the whole set of classes the race can play.
 *
 * @author adrianpmdev
 */
public record RaceUpdateRequest(
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
