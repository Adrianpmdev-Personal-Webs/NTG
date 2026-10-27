package com.adrianpmdev.pokemon.features.players.infrastructure.persistence.repository;

import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Players.
 *
 * @author adrianpmdev
 */
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    /**
     * Every player by name.
     *
     * @return players
     */
    List<Player> findAllByOrderByNameAsc();

    /**
     * Whether another player already uses a name (names are unique, case aside).
     *
     * @param name name
     * @param id   player to ignore (the one being edited), or null
     * @return true when the name is taken
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /**
     * Whether a name is taken.
     *
     * @param name name
     * @return true when the name is taken
     */
    boolean existsByNameIgnoreCase(String name);
}
