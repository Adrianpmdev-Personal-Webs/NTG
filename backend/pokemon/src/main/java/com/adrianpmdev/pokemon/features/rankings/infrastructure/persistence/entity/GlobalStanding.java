package com.adrianpmdev.pokemon.features.rankings.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "global_standings", schema = "pokemon")
public class GlobalStanding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private java.util.UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private Short totalTournaments = 0;

    @Column(nullable = false)
    @Builder.Default
    private Short tournamentsWon = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Column(name = "ranking_position")
    private Short rankingPosition;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}