package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.BracketMatch;

import java.util.UUID;
import java.util.List;

@Repository
public interface BracketMatchRepository extends JpaRepository<BracketMatch, UUID> {
    List<BracketMatch> findByBracketStageId(UUID bracketStageId);
    List<BracketMatch> findByTournamentId(UUID tournamentId);
    List<BracketMatch> findByBracketStageIdAndStatus(UUID bracketStageId, String status);
}