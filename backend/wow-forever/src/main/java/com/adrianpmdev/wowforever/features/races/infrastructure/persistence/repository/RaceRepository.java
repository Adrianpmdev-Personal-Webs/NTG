package com.adrianpmdev.wowforever.features.races.infrastructure.persistence.repository;

import com.adrianpmdev.wowforever.features.races.infrastructure.persistence.entity.Race;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for race persistence operations. The classes of a race are loaded in the same query.
 *
 * @author adrianpmdev
 */
@Repository
public interface RaceRepository extends JpaRepository<Race, UUID> {

    @EntityGraph(attributePaths = "classes")
    List<Race> findAllByOrderBySortOrderAsc();

    @EntityGraph(attributePaths = "classes")
    Optional<Race> findWithClassesById(UUID id);

    boolean existsBySlug(String slug);
}
