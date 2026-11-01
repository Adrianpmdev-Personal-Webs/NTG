package com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A character of a player. The class, the race and the professions are slugs of the game data.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "characters", schema = "wow_forever")
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Platform account (core user id). A plain column: the game schema does not depend on core. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 12, nullable = false)
    private String name;

    @Column(name = "class_slug", length = 40, nullable = false)
    private String classSlug;

    @Column(name = "race_slug", length = 40, nullable = false)
    private String raceSlug;

    @Column(nullable = false)
    private short level;

    /** List of the professions: {@code [{ "slug": "alchemy", "skill": 300 }]}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode professions;

    @Column(length = 200)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
