package com.adrianpmdev.lol.features.players.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * The Riot account of a platform user, with their official rank and how the download of their games is going.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "accounts", schema = "lol")
public class LolAccount {

    /** Waiting for an admin to start the download. */
    public static final String PENDING = "PENDING";
    /** The download was started and has not finished. */
    public static final String LOADING = "LOADING";
    /** Every game is stored; from now on only new ones are asked for. */
    public static final String COMPLETE = "COMPLETE";
    /** The download failed; an admin can start it again. */
    public static final String ERROR = "ERROR";

    /** Platform account (core user id). A plain column: the game schema does not depend on core. */
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "game_name", length = 16, nullable = false)
    private String gameName;

    @Column(name = "tag_line", length = 5, nullable = false)
    private String tagLine;

    @Column(length = 100, nullable = false, unique = true)
    private String puuid;

    @Column(length = 10, nullable = false)
    private String state;

    /** List of the official ranks, as the backend last read them. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode ranks;

    @Column(name = "synced_until")
    private Instant syncedUntil;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "last_error", length = 300)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
