package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;

import java.util.List;
import java.util.UUID;

/**
 * Repository for tournament persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    List<Tournament> findByLockeId(UUID lockeId);

    List<Tournament> findByStatus(String status);

    List<Tournament> findByNameContainingIgnoreCase(String name);
}