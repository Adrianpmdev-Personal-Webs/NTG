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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "rewards", schema = "pokemon", uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "position"}))
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Tournament tournament;

    @Column(nullable = false)
    private Short position;

    @Column(length = 100, nullable = false, name = "reward_name")
    private String rewardName;

    @Column(name = "reward_value")
    private Short rewardValue;

    @Column(length = 500, name = "image_url")
    private String imageUrl;
}