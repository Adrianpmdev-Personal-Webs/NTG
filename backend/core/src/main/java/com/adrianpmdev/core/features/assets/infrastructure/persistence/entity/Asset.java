package com.adrianpmdev.core.features.assets.infrastructure.persistence.entity;

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
 * Public picture uploaded by an admin: the object that holds it in the public bucket and its size.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "asset", schema = "core")
public class Asset {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "key", nullable = false, length = 150)
    private String key;

    @Column(name = "object_name", nullable = false, length = 200)
    private String objectName;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Creates the record of a picture with no file yet.
     *
     * @param key picture key
     */
    public Asset(String key) {
        this.key = key;
    }
}
