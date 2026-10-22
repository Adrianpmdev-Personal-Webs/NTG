package com.adrianpmdev.wowforever.features.classes.api.mapper;

import com.adrianpmdev.wowforever.features.classes.api.dto.request.WowClassCreateRequest;
import com.adrianpmdev.wowforever.features.classes.api.dto.request.WowClassUpdateRequest;
import com.adrianpmdev.wowforever.features.classes.api.dto.response.WowClassResponse;
import com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.entity.WowClass;

/**
 * Mapping utility for converting class entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class WowClassMapper {

    private WowClassMapper() {
    }

    public static WowClassResponse toResponse(WowClass wowClass) {
        return new WowClassResponse(
                wowClass.getId(),
                wowClass.getSlug(),
                wowClass.getName(),
                wowClass.getIcon(),
                wowClass.getSortOrder(),
                wowClass.getUpdatedAt()
        );
    }

    public static WowClass toEntity(WowClassCreateRequest request) {
        WowClass wowClass = new WowClass();
        wowClass.setSlug(request.slug());
        updateEntity(wowClass, request.name(), request.icon(), request.sortOrder());

        return wowClass;
    }

    public static void updateEntity(WowClass wowClass, WowClassUpdateRequest request) {
        updateEntity(wowClass, request.name(), request.icon(), request.sortOrder());
    }

    private static void updateEntity(WowClass wowClass, String name, String icon, int sortOrder) {
        wowClass.setName(name.trim());
        wowClass.setIcon(icon.trim());
        wowClass.setSortOrder(sortOrder);
    }
}
