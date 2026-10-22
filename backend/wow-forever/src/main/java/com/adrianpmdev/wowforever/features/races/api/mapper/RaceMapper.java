package com.adrianpmdev.wowforever.features.races.api.mapper;

import com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.entity.WowClass;
import com.adrianpmdev.wowforever.features.races.api.dto.response.RaceResponse;
import com.adrianpmdev.wowforever.features.races.infrastructure.persistence.entity.Race;

import java.util.Comparator;

/**
 * Mapping utility for converting race entities into response DTOs.
 *
 * @author adrianpmdev
 */
public final class RaceMapper {

    private RaceMapper() {
    }

    public static RaceResponse toResponse(Race race) {
        return new RaceResponse(
                race.getId(),
                race.getSlug(),
                race.getName(),
                race.getFaction(),
                race.getIcon(),
                race.getSortOrder(),
                race.getClasses().stream()
                        .sorted(Comparator.comparingInt(WowClass::getSortOrder))
                        .map(WowClass::getId)
                        .toList(),
                race.getUpdatedAt()
        );
    }
}
