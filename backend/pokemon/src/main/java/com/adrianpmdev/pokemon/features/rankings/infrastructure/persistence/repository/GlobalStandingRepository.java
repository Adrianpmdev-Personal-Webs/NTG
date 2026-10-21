package com.adrianpmdev.pokemon.features.rankings.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.rankings.infrastructure.persistence.entity.GlobalStanding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for global standing persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface GlobalStandingRepository extends JpaRepository<GlobalStanding, UUID> {

    Optional<GlobalStanding> findByUserId(UUID userId);

    List<GlobalStanding> findAllByOrderByRankingPositionAsc();
}