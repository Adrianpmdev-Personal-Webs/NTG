package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentStanding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tournament standing persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface TournamentStandingRepository extends JpaRepository<TournamentStanding, UUID> {

    Optional<TournamentStanding> findByTournamentIdAndTournamentParticipantId(
            UUID tournamentId,
            UUID participantId
    );

    List<TournamentStanding> findByTournamentIdOrderByFinalPositionAsc(UUID tournamentId);
}