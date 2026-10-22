package com.adrianpmdev.wowforever.features.races.application.service;

import com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.entity.WowClass;
import com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.repository.WowClassRepository;
import com.adrianpmdev.wowforever.features.races.api.dto.request.RaceCreateRequest;
import com.adrianpmdev.wowforever.features.races.api.dto.request.RaceUpdateRequest;
import com.adrianpmdev.wowforever.features.races.api.dto.response.RaceResponse;
import com.adrianpmdev.wowforever.features.races.api.mapper.RaceMapper;
import com.adrianpmdev.wowforever.features.races.infrastructure.persistence.entity.Faction;
import com.adrianpmdev.wowforever.features.races.infrastructure.persistence.entity.Race;
import com.adrianpmdev.wowforever.features.races.infrastructure.persistence.repository.RaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing races and the classes each one can play.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class RaceService {

    private final RaceRepository raceRepository;
    private final WowClassRepository wowClassRepository;

    @Transactional(readOnly = true)
    public List<RaceResponse> getAll() {
        return raceRepository.findAllByOrderBySortOrderAsc().stream().map(RaceMapper::toResponse).toList();
    }

    @Transactional
    public RaceResponse create(RaceCreateRequest request) {
        if (raceRepository.existsBySlug(request.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Race '" + request.slug() + "' already exists");
        }

        Race race = new Race();
        race.setSlug(request.slug());
        apply(race, request.name(), request.faction(), request.icon(), request.sortOrder(), request.classIds());

        return RaceMapper.toResponse(raceRepository.save(race));
    }

    @Transactional
    public RaceResponse update(UUID id, RaceUpdateRequest request) {
        Race race = raceRepository.findWithClassesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        apply(race, request.name(), request.faction(), request.icon(), request.sortOrder(), request.classIds());

        return RaceMapper.toResponse(raceRepository.save(race));
    }

    @Transactional
    public void delete(UUID id) {
        Race race = raceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        raceRepository.delete(race);
        // Flush now so a foreign key violation (data still using the race) is reported as a 409.
        raceRepository.flush();
    }

    private void apply(Race race, String name, Faction faction, String icon, int sortOrder, List<UUID> classIds) {
        race.setName(name.trim());
        race.setFaction(faction);
        race.setIcon(icon.trim());
        race.setSortOrder(sortOrder);
        race.getClasses().clear();
        race.getClasses().addAll(resolveClasses(classIds));
    }

    private Set<WowClass> resolveClasses(List<UUID> classIds) {
        Set<UUID> wanted = new HashSet<>(classIds);
        List<WowClass> found = wowClassRepository.findAllById(wanted);

        if (found.size() != wanted.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some of the classes do not exist");
        }

        return new HashSet<>(found);
    }
}
