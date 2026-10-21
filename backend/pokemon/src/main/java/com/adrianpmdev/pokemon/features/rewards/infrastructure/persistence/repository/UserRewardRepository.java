package com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity.UserReward;

import java.util.List;
import java.util.UUID;

/**
 * Repository for user reward persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, UUID> {

    List<UserReward> findByUserId(UUID userId);

    List<UserReward> findByTournamentId(UUID tournamentId);

    List<UserReward> findByUserIdAndTournamentId(UUID userId, UUID tournamentId);
}