package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "tournament_standings", schema = "pokemon", uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "tournament_participant_id"}))
public class TournamentStanding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_participant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TournamentParticipant tournamentParticipant;

    @Column(name = "final_position")
    private Short finalPosition;

    @Column(nullable = false)
    @Builder.Default
    private Short totalWins = 0;

    @Column(nullable = false)
    @Builder.Default
    private Short totalLosses = 0;

    @Column(nullable = false)
    @Builder.Default
    private Short totalPoints = 0;

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