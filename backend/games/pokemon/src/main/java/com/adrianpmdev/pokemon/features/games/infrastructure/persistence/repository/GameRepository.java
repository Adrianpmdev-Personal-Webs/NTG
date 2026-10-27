package com.adrianpmdev.pokemon.features.games.infrastructure.persistence.repository;

import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Games.
 *
 * @author adrianpmdev
 */
public interface GameRepository extends JpaRepository<Game, UUID> {

    /**
     * Every game, oldest first (the order they were added).
     *
     * @return games
     */
    List<Game> findAllByOrderByCreatedAtAsc();
}
