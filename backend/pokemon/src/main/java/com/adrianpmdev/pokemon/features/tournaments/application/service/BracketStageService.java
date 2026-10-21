package com.adrianpmdev.pokemon.features.tournaments.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketStageCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketStageUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.BracketStageResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.BracketStage;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.api.mapper.BracketStageMapper;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.BracketStageRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing bracket stages.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class BracketStageService {

    private final BracketStageRepository stageRepository;
    private final TournamentRepository tournamentRepository;

    public BracketStageResponse createStage(BracketStageCreateRequest request) {
        Tournament tournament = getTournamentEntityById(request.tournamentId());
        BracketStage stage = BracketStageMapper.toEntity(request, tournament);
        return BracketStageMapper.toResponse(stageRepository.save(stage));
    }

    public BracketStageResponse getStageById(UUID id) {
        return BracketStageMapper.toResponse(getStageEntityById(id));
    }

    public List<BracketStageResponse> getAllStages() {
        return stageRepository.findAll()
                .stream()
                .map(BracketStageMapper::toResponse)
                .toList();
    }

    public List<BracketStageResponse> getStagesByTournamentId(UUID tournamentId) {
        return stageRepository.findByTournamentIdOrderByStageOrder(tournamentId)
                .stream()
                .map(BracketStageMapper::toResponse)
                .toList();
    }

    public BracketStageResponse updateStage(UUID id, BracketStageUpdateRequest request) {
        BracketStage stage = getStageEntityById(id);
        Tournament tournament = getTournamentEntityByIdOrNull(request.tournamentId());

        BracketStageMapper.updateEntity(stage, request, tournament);

        return BracketStageMapper.toResponse(stageRepository.save(stage));
    }

    public void deleteStage(UUID id) {
        BracketStage stage = getStageEntityById(id);
        stageRepository.delete(stage);
    }

    private BracketStage getStageEntityById(UUID id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bracket stage not found"));
    }

    private Tournament getTournamentEntityById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
    }

    private Tournament getTournamentEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getTournamentEntityById(id);
    }
}