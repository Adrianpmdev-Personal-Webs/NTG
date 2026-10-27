package com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository;

import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Randomlockes.
 *
 * @author adrianpmdev
 */
public interface LockeRepository extends JpaRepository<Locke, UUID> {

    /**
     * Every randomlocke with its roster, newest first.
     *
     * @return lockes
     */
    @EntityGraph(attributePaths = "roster")
    List<Locke> findAllByOrderByCreatedAtDesc();

    /**
     * One randomlocke with its roster.
     *
     * @param id locke id
     * @return the locke
     */
    @EntityGraph(attributePaths = "roster")
    Optional<Locke> findWithRosterById(UUID id);
}
