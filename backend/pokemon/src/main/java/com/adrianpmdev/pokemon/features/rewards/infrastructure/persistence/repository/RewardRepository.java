package com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity.Reward;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for reward persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface RewardRepository extends JpaRepository<Reward, UUID> {

    List<Reward> findByTournamentId(UUID tournamentId);

    Optional<Reward> findByTournamentIdAndPosition(UUID tournamentId, Short position);
}