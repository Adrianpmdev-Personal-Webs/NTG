package com.adrianpmdev.pokemon.features.rankings.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.rankings.api.dto.request.GlobalStandingCreateRequest;
import com.adrianpmdev.pokemon.features.rankings.api.dto.request.GlobalStandingUpdateRequest;
import com.adrianpmdev.pokemon.features.rankings.api.dto.response.GlobalStandingResponse;
import com.adrianpmdev.pokemon.features.rankings.infrastructure.persistence.entity.GlobalStanding;
import com.adrianpmdev.pokemon.features.rankings.api.mapper.GlobalStandingMapper;
import com.adrianpmdev.pokemon.features.rankings.infrastructure.persistence.repository.GlobalStandingRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing global standings.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class GlobalStandingService {

    private final GlobalStandingRepository standingRepository;

    public GlobalStandingResponse createStanding(GlobalStandingCreateRequest request) {
        GlobalStanding standing = GlobalStandingMapper.toEntity(request);
        return GlobalStandingMapper.toResponse(standingRepository.save(standing));
    }

    public GlobalStandingResponse getStandingById(UUID id) {
        return GlobalStandingMapper.toResponse(getStandingEntityById(id));
    }

    public List<GlobalStandingResponse> getAllStandings() {
        return standingRepository.findAll()
                .stream()
                .map(GlobalStandingMapper::toResponse)
                .toList();
    }

    public GlobalStandingResponse getStandingByUserId(UUID userId) {
        return GlobalStandingMapper.toResponse(getStandingEntityByUserId(userId));
    }

    public List<GlobalStandingResponse> getRankingByPosition() {
        return standingRepository.findAllByOrderByRankingPositionAsc()
                .stream()
                .map(GlobalStandingMapper::toResponse)
                .toList();
    }

    public GlobalStandingResponse updateStanding(UUID id, GlobalStandingUpdateRequest request) {
        GlobalStanding standing = getStandingEntityById(id);

        GlobalStandingMapper.updateEntity(standing, request);

        return GlobalStandingMapper.toResponse(standingRepository.save(standing));
    }

    public void deleteStanding(UUID id) {
        GlobalStanding standing = getStandingEntityById(id);
        standingRepository.delete(standing);
    }

    private GlobalStanding getStandingEntityById(UUID id) {
        return standingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Global standing not found"));
    }

    private GlobalStanding getStandingEntityByUserId(UUID userId) {
        return standingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Global standing not found"));
    }
}