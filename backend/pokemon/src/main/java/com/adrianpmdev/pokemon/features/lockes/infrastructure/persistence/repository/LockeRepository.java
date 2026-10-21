package com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;

import java.util.List;
import java.util.UUID;

/**
 * Repository for locke persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface LockeRepository extends JpaRepository<Locke, UUID> {

    List<Locke> findByGameId(UUID gameId);

    List<Locke> findByNameContainingIgnoreCase(String name);
}