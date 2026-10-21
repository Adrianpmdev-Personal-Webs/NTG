package com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
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
@Table(name = "user_rewards", schema = "pokemon")
public class UserReward {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Reward reward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Tournament tournament;

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private Instant claimedAt;

    @PrePersist
    protected void onCreate() {
        claimedAt = Instant.now();
    }
}