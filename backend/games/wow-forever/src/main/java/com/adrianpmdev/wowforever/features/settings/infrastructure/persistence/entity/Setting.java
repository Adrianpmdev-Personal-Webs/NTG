package com.adrianpmdev.wowforever.features.settings.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Module setting stored as key/value text (for example the maximum level). Keys are created in the database.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "setting", schema = "wow_forever")
public class Setting {

    @Id
    @Column(name = "key", length = 60, nullable = false)
    private String key;

    @Column(length = 255, nullable = false)
    private String value;

    @Column(length = 255)
    private String description;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = Instant.now();
    }
}
