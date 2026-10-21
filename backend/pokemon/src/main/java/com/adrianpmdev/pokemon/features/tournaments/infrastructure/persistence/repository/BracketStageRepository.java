package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.BracketStage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for bracket stage persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface BracketStageRepository extends JpaRepository<BracketStage, UUID> {

    List<BracketStage> findByTournamentIdOrderByStageOrder(UUID tournamentId);

    Optional<BracketStage> findByTournamentIdAndStageOrder(UUID tournamentId, Short stageOrder);
}