package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.GroupParticipant;

import java.util.List;
import java.util.UUID;

/**
 * Repository for group participant persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface GroupParticipantRepository extends JpaRepository<GroupParticipant, UUID> {

    List<GroupParticipant> findByGroupId(UUID groupId);

    List<GroupParticipant> findByGroupIdOrderByGroupPositionAsc(UUID groupId);
}