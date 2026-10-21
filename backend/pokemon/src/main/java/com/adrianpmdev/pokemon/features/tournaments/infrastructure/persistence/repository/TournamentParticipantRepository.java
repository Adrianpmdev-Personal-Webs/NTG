package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tournament participant persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, UUID> {

    List<TournamentParticipant> findByTournamentId(UUID tournamentId);

    Optional<TournamentParticipant> findByTournamentIdAndUserId(UUID tournamentId, UUID userId);

    List<TournamentParticipant> findByUserId(UUID userId);

    List<TournamentParticipant> findByTournamentIdAndStatus(UUID tournamentId, String status);
}