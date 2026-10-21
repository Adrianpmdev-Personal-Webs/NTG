package com.adrianpmdev.core.features.tags.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Tag that grants access to a game (or any other area) when assigned to a user.
 * The key is immutable and is also used to build the Spring authority {@code TAG_<KEY>}.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tag", schema = "core")
public class Tag {
    private static final int KEY_MAX_LENGTH = 30;
    private static final int NAME_MAX_LENGTH = 60;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "key", nullable = false, length = KEY_MAX_LENGTH)
    private String key;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Creates a new tag.
     *
     * @param key         unique lowercase key
     * @param name        display name
     * @param description optional description
     */
    public Tag(String key, String name, String description) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
    }
}