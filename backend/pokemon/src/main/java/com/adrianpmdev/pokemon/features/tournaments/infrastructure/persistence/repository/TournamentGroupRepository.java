package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tournament group persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface TournamentGroupRepository extends JpaRepository<TournamentGroup, UUID> {

    List<TournamentGroup> findByTournamentId(UUID tournamentId);

    Optional<TournamentGroup> findByTournamentIdAndGroupLetter(UUID tournamentId, String groupLetter);
}