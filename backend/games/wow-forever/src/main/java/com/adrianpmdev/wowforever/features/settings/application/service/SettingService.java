package com.adrianpmdev.wowforever.features.settings.application.service;

import com.adrianpmdev.wowforever.features.settings.api.dto.request.SettingUpdateRequest;
import com.adrianpmdev.wowforever.features.settings.api.dto.response.SettingResponse;
import com.adrianpmdev.wowforever.features.settings.api.mapper.SettingMapper;
import com.adrianpmdev.wowforever.features.settings.infrastructure.persistence.entity.Setting;
import com.adrianpmdev.wowforever.features.settings.infrastructure.persistence.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service for reading and changing module settings.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingRepository settingRepository;

    @Transactional(readOnly = true)
    public List<SettingResponse> getAll() {
        return settingRepository.findAllByOrderByKeyAsc().stream().map(SettingMapper::toResponse).toList();
    }

    @Transactional
    public SettingResponse update(String key, SettingUpdateRequest request) {
        Setting setting = settingRepository.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Setting not found"));
        setting.setValue(request.value().trim());

        return SettingMapper.toResponse(settingRepository.save(setting));
    }
}
