package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity;

import com.adrianpmdev.pokemon.features.tournaments.domain.MatchStage;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.converter.EnumConverters;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A best-of-3 series between two tournament players, placed by stage, round and slot. Its score and
 * winner are counted from its games.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "matches", schema = "pokemon")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Convert(converter = EnumConverters.StageConverter.class)
    @Column(length = 12, nullable = false)
    private MatchStage stage;

    @Column(nullable = false)
    private short round;

    @Column(nullable = false)
    private short slot;

    /** Null while waiting for a previous result, or for the empty side of a bye. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id")
    private TournamentPlayer player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id")
    private TournamentPlayer player2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("gameNumber")
    private List<MatchGame> games = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
