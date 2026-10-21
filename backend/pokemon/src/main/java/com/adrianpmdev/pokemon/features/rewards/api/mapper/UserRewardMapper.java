package com.adrianpmdev.pokemon.features.rewards.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.rewards.api.dto.request.UserRewardCreateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.request.UserRewardUpdateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.response.UserRewardResponse;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity.Reward;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity.UserReward;

/**
 * Mapping utility for converting user reward entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class UserRewardMapper {

    private UserRewardMapper() {
    }

    public static UserRewardResponse toResponse(UserReward userReward) {
        return new UserRewardResponse(
                userReward.getId(),
                userReward.getUserId(),
                getRewardId(userReward.getReward()),
                getTournamentId(userReward.getTournament()),
                userReward.getClaimedAt()
        );
    }

    public static UserReward toEntity(
            UserRewardCreateRequest request,
            Reward reward,
            Tournament tournament
    ) {
        UserReward userReward = new UserReward();
        userReward.setUserId(request.userId());
        userReward.setReward(reward);
        userReward.setTournament(tournament);
        return userReward;
    }

    public static void updateEntity(
            UserReward userReward,
            UserRewardUpdateRequest request,
            Reward reward,
            Tournament tournament
    ) {
        if (request.userId() != null) {
            userReward.setUserId(request.userId());
        }

        if (reward != null) {
            userReward.setReward(reward);
        }

        if (tournament != null) {
            userReward.setTournament(tournament);
        }
    }

    private static UUID getRewardId(Reward reward) {
        return reward == null ? null : reward.getId();
    }

    private static UUID getTournamentId(Tournament tournament) {
        return tournament == null ? null : tournament.getId();
    }
}