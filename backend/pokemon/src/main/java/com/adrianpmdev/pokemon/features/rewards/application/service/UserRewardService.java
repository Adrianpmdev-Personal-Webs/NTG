package com.adrianpmdev.pokemon.features.rewards.application.service;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.rewards.api.dto.request.UserRewardCreateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.request.UserRewardUpdateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.response.UserRewardResponse;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity.Reward;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity.UserReward;
import com.adrianpmdev.pokemon.features.rewards.api.mapper.UserRewardMapper;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.repository.RewardRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.repository.UserRewardRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing user rewards.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class UserRewardService {

    private final UserRewardRepository userRewardRepository;
    private final RewardRepository rewardRepository;
    private final TournamentRepository tournamentRepository;

    public UserRewardResponse claimReward(UserRewardCreateRequest request) {
        Reward reward = getRewardEntityById(request.rewardId());
        Tournament tournament = getTournamentEntityById(request.tournamentId());

        UserReward userReward = UserRewardMapper.toEntity(request, reward, tournament);

        return UserRewardMapper.toResponse(userRewardRepository.save(userReward));
    }

    public UserRewardResponse getRewardById(UUID id) {
        UserReward userReward = getUserRewardEntityById(id);
        return UserRewardMapper.toResponse(userReward);
    }

    public List<UserRewardResponse> getAllRewards() {
        return userRewardRepository.findAll()
                .stream()
                .map(UserRewardMapper::toResponse)
                .toList();
    }

    public List<UserRewardResponse> getRewardsByUserId(UUID userId) {
        return userRewardRepository.findByUserId(userId)
                .stream()
                .map(UserRewardMapper::toResponse)
                .toList();
    }

    public List<UserRewardResponse> getRewardsByTournamentId(UUID tournamentId) {
        return userRewardRepository.findByTournamentId(tournamentId)
                .stream()
                .map(UserRewardMapper::toResponse)
                .toList();
    }

    public List<UserRewardResponse> getRewardsByUserAndTournament(UUID userId, UUID tournamentId) {
        return userRewardRepository.findByUserIdAndTournamentId(userId, tournamentId)
                .stream()
                .map(UserRewardMapper::toResponse)
                .toList();
    }

    public UserRewardResponse updateReward(UUID id, UserRewardUpdateRequest request) {
        UserReward userReward = getUserRewardEntityById(id);
        Reward reward = getRewardEntityByIdOrNull(request.rewardId());
        Tournament tournament = getTournamentEntityByIdOrNull(request.tournamentId());

        UserRewardMapper.updateEntity(userReward, request, reward, tournament);

        return UserRewardMapper.toResponse(userRewardRepository.save(userReward));
    }

    public void deleteReward(UUID id) {
        UserReward userReward = getUserRewardEntityById(id);
        userRewardRepository.delete(userReward);
    }

    private UserReward getUserRewardEntityById(UUID id) {
        return userRewardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User reward not found"));
    }

    private Reward getRewardEntityById(UUID id) {
        return rewardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reward not found"));
    }

    private Reward getRewardEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getRewardEntityById(id);
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