package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.GroupMatch;

import java.util.List;
import java.util.UUID;

/**
 * Repository for group match persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface GroupMatchRepository extends JpaRepository<GroupMatch, UUID> {

    List<GroupMatch> findByGroupId(UUID groupId);

    List<GroupMatch> findByGroupIdAndStatus(UUID groupId, String status);
}