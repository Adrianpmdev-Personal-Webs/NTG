package com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository;

import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.LockeDeath;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pokémon lost in randomlockes.
 *
 * @author adrianpmdev
 */
public interface LockeDeathRepository extends JpaRepository<LockeDeath, UUID> {

    /**
     * The Pokémon lost in a randomlocke, with their players, oldest first.
     *
     * @param lockeId locke id
     * @return deaths
     */
    @EntityGraph(attributePaths = "player")
    List<LockeDeath> findByLockeIdOrderByCreatedAtAsc(UUID lockeId);

    /**
     * One death of a randomlocke, with its player.
     *
     * @param id      death id
     * @param lockeId locke id
     * @return the death
     */
    @EntityGraph(attributePaths = "player")
    Optional<LockeDeath> findByIdAndLockeId(UUID id, UUID lockeId);

    /**
     * How many Pokémon a player lost in every randomlocke.
     *
     * @param playerId player id
     * @return number of deaths
     */
    long countByPlayerId(UUID playerId);
}
