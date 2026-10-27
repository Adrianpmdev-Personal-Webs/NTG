package com.adrianpmdev.core.features.catalog.infrastructure.persistence.entity;

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
 * A game of the catalog shown by the web clients. The key is the game id, the key of the tag that unlocks it and its
 * URL segment; it never changes.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "game", schema = "core")
public class CatalogGame {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "key", nullable = false, length = 30)
    private String key;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "upcoming", nullable = false)
    private boolean upcoming;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Creates a game.
     *
     * @param key       unique lowercase key
     * @param name      display name
     * @param upcoming  whether it is still coming soon
     * @param sortOrder position in the lists
     */
    public CatalogGame(String key, String name, boolean upcoming, short sortOrder) {
        this.key = key;
        this.name = name;
        this.upcoming = upcoming;
        this.sortOrder = sortOrder;
        this.createdAt = Instant.now();
    }
}
