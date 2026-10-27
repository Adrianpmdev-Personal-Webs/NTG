package com.adrianpmdev.core.features.assets.infrastructure.persistence.repository;

import com.adrianpmdev.core.features.assets.infrastructure.persistence.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for the {@link Asset} entity.
 *
 * @author adrianpmdev
 */
@Repository
public interface AssetRepository extends JpaRepository<Asset, String> {
}
