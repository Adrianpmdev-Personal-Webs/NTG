package com.adrianpmdev.core.features.tags.infrastructure.persistence.repository;

import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for the {@link Tag} entity.
 *
 * @author adrianpmdev
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, String> {

    /**
     * Lists all tags ordered by key.
     *
     * @return tags
     */
    List<Tag> findAllByOrderByKeyAsc();
}