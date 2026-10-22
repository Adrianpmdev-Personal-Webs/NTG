package com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.repository;

import com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.entity.WowClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for class persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface WowClassRepository extends JpaRepository<WowClass, UUID> {

    List<WowClass> findAllByOrderBySortOrderAsc();

    boolean existsBySlug(String slug);
}
