package com.adrianpmdev.pokemon.features.rewards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.rewards.api.dto.request.RewardCreateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.request.RewardUpdateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.response.RewardResponse;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity.Reward;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.rewards.api.mapper.RewardMapper;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.repository.RewardRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing rewards.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final TournamentRepository tournamentRepository;

    public RewardResponse createReward(RewardCreateRequest request) {
        Tournament tournament = getTournamentEntityById(request.tournamentId());
        Reward reward = RewardMapper.toEntity(request, tournament);
        return RewardMapper.toResponse(rewardRepository.save(reward));
    }

    public RewardResponse getRewardById(UUID id) {
        return RewardMapper.toResponse(getRewardEntityById(id));
    }

    public List<RewardResponse> getAllRewards() {
        return rewardRepository.findAll()
                .stream()
                .map(RewardMapper::toResponse)
                .toList();
    }

    public List<RewardResponse> getRewardsByTournamentId(UUID tournamentId) {
        return rewardRepository.findByTournamentId(tournamentId)
                .stream()
                .map(RewardMapper::toResponse)
                .toList();
    }

    public RewardResponse updateReward(UUID id, RewardUpdateRequest request) {
        Reward reward = getRewardEntityById(id);
        Tournament tournament = getTournamentEntityByIdOrNull(request.tournamentId());

        RewardMapper.updateEntity(reward, request, tournament);

        return RewardMapper.toResponse(rewardRepository.save(reward));
    }

    public void deleteReward(UUID id) {
        Reward reward = getRewardEntityById(id);
        rewardRepository.delete(reward);
    }

    private Reward getRewardEntityById(UUID id) {
        return rewardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reward not found"));
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