package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "group_participants", schema = "pokemon", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "tournament_participant_id"}))
public class GroupParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TournamentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_participant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TournamentParticipant tournamentParticipant;

    @Column(name = "group_position")
    private Short groupPosition;

    @Column(nullable = false)
    @Builder.Default
    private Short wins = 0;

    @Column(nullable = false)
    @Builder.Default
    private Short losses = 0;

    @Column(nullable = false)
    @Builder.Default
    private Short points = 0;
}