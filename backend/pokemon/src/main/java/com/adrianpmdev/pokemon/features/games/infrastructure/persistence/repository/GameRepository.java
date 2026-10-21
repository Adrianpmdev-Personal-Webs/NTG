package com.adrianpmdev.pokemon.features.games.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity.Game;

import java.util.List;
import java.util.UUID;

/**
 * Repository for game persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    List<Game> findByNameContainingIgnoreCase(String name);

    boolean existsByName(String name);
}