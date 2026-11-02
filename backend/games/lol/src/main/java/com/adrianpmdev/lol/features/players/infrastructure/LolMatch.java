package com.adrianpmdev.lol.features.players.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * The summary of a game of a player. It is only read through JPA; {@link LolMatchRepository} inserts it with plain SQL
 * so that a game stored twice is simply ignored.
 *
 * @author adrianpmdev
 */
@Getter
@NoArgsConstructor
@Entity
@IdClass(LolMatch.Key.class)
@Table(name = "matches", schema = "lol")
public class LolMatch {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "match_id")
    private String matchId;

    @Column(name = "queue_id")
    private int queueId;

    @Column(name = "played_at")
    private Instant playedAt;

    @Column(name = "duration_seconds")
    private int durationSeconds;

    @Column(name = "champion_id")
    private int championId;

    @Column(name = "champion_name")
    private String championName;

    private boolean win;

    private boolean remake;

    private int kills;

    private int deaths;

    private int assists;

    private int cs;

    private int damage;

    private int gold;

    private int vision;

    @Column(name = "champion_level")
    private int championLevel;

    private String items;

    private int spell1;

    private int spell2;

    /**
     * Identity of a row.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        private UUID userId;
        private String matchId;
    }
}
