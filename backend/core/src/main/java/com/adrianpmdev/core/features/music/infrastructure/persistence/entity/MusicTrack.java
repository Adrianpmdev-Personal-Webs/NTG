package com.adrianpmdev.core.features.music.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A song uploaded by an admin to the music of an area (the platform or a game): the object that holds it in the
 * public bucket and its place in the playlist of the area.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "music_track", schema = "core")
public class MusicTrack {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "area", nullable = false, length = 50)
    private String area;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "object_name", nullable = false, length = 200)
    private String objectName;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
