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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
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
@Entity
@Table(name = "bracket_matches", schema = "pokemon", uniqueConstraints = @UniqueConstraint(columnNames = {"bracket_stage_id", "match_number"}))
public class BracketMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bracket_stage_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private BracketStage bracketStage;

    @Column(nullable = false, name = "match_number")
    private Short matchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TournamentParticipant participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TournamentParticipant participant2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TournamentParticipant winner;

    @Column(length = 500, name = "replay_url")
    private String replayUrl;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}