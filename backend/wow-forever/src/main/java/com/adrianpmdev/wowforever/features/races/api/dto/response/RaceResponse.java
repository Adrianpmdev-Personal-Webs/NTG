package com.adrianpmdev.wowforever.features.races.api.dto.response;

import com.adrianpmdev.wowforever.features.races.infrastructure.persistence.entity.Faction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for race data, including the ids of the classes it can play.
 *
 * @author adrianpmdev
 */
public record RaceResponse(
        UUID id,
        String slug,
        String name,
        Faction faction,
        String icon,
        int sortOrder,
        List<UUID> classIds,
        Instant updatedAt
) {}
