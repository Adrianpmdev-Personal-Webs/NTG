package com.adrianpmdev.wowforever.features.classes.application.service;

import com.adrianpmdev.wowforever.features.classes.api.dto.request.WowClassCreateRequest;
import com.adrianpmdev.wowforever.features.classes.api.dto.request.WowClassUpdateRequest;
import com.adrianpmdev.wowforever.features.classes.api.dto.response.WowClassResponse;
import com.adrianpmdev.wowforever.features.classes.api.mapper.WowClassMapper;
import com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.entity.WowClass;
import com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.repository.WowClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing classes.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class WowClassService {

    private final WowClassRepository wowClassRepository;

    @Transactional(readOnly = true)
    public List<WowClassResponse> getAll() {
        return wowClassRepository.findAllByOrderBySortOrderAsc().stream().map(WowClassMapper::toResponse).toList();
    }

    @Transactional
    public WowClassResponse create(WowClassCreateRequest request) {
        if (wowClassRepository.existsBySlug(request.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class '" + request.slug() + "' already exists");
        }

        return WowClassMapper.toResponse(wowClassRepository.save(WowClassMapper.toEntity(request)));
    }

    @Transactional
    public WowClassResponse update(UUID id, WowClassUpdateRequest request) {
        WowClass wowClass = find(id);
        WowClassMapper.updateEntity(wowClass, request);

        return WowClassMapper.toResponse(wowClassRepository.save(wowClass));
    }

    @Transactional
    public void delete(UUID id) {
        wowClassRepository.delete(find(id));
        // Flush now so a foreign key violation (data still using the class) is reported as a 409.
        wowClassRepository.flush();
    }

    private WowClass find(UUID id) {
        return wowClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
    }
}
