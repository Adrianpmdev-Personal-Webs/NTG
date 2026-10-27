package com.adrianpmdev.core.features.catalog.infrastructure.persistence.repository;

import com.adrianpmdev.core.features.catalog.infrastructure.persistence.entity.CatalogGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for the {@link CatalogGame} entity.
 *
 * @author adrianpmdev
 */
@Repository
public interface CatalogGameRepository extends JpaRepository<CatalogGame, String> {

    /**
     * Lists the catalog in display order.
     *
     * @return games
     */
    List<CatalogGame> findAllByOrderBySortOrderAscKeyAsc();
}
