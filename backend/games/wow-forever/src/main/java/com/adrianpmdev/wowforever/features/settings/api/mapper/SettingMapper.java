package com.adrianpmdev.wowforever.features.settings.api.mapper;

import com.adrianpmdev.wowforever.features.settings.api.dto.response.SettingResponse;
import com.adrianpmdev.wowforever.features.settings.infrastructure.persistence.entity.Setting;

/**
 * Mapping utility for converting setting entities into response DTOs.
 *
 * @author adrianpmdev
 */
public final class SettingMapper {

    private SettingMapper() {
    }

    public static SettingResponse toResponse(Setting setting) {
        return new SettingResponse(setting.getKey(), setting.getValue(), setting.getDescription(), setting.getUpdatedAt());
    }
}
