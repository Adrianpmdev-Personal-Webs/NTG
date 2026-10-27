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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One game of a series, with its replay.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "match_games", schema = "pokemon")
public class MatchGame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /** 1, 2 or 3. */
    @Column(name = "game_number", nullable = false)
    private short gameNumber;

    /** 1 or 2: which side of the series won the game. */
    @Column(name = "winner_side", nullable = false)
    private short winnerSide;

    @Column(name = "replay_url", length = 500)
    private String replayUrl;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;
}
