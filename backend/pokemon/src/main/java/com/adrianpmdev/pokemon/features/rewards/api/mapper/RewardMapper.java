package com.adrianpmdev.pokemon.features.rewards.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.rewards.api.dto.request.RewardCreateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.request.RewardUpdateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.response.RewardResponse;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity.Reward;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;

/**
 * Mapping utility for converting reward entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class RewardMapper {

    private RewardMapper() {
    }

    public static RewardResponse toResponse(Reward reward) {
        return new RewardResponse(
                reward.getId(),
                getTournamentId(reward.getTournament()),
                reward.getPosition(),
                reward.getRewardName(),
                reward.getRewardValue(),
                reward.getImageUrl()
        );
    }

    public static Reward toEntity(RewardCreateRequest request, Tournament tournament) {
        Reward reward = new Reward();
        reward.setTournament(tournament);
        reward.setPosition(request.position());
        reward.setRewardName(request.rewardName());
        reward.setRewardValue(request.rewardValue());
        reward.setImageUrl(request.imageUrl());
        return reward;
    }

    public static void updateEntity(Reward reward, RewardUpdateRequest request, Tournament tournament) {
        if (tournament != null) {
            reward.setTournament(tournament);
        }

        if (request.position() != null) {
            reward.setPosition(request.position());
        }

        if (request.rewardName() != null) {
            reward.setRewardName(request.rewardName());
        }

        if (request.rewardValue() != null) {
            reward.setRewardValue(request.rewardValue());
        }

        if (request.imageUrl() != null) {
            reward.setImageUrl(request.imageUrl());
        }
    }

    private static UUID getTournamentId(Tournament tournament) {
        return tournament == null ? null : tournament.getId();
    }
}