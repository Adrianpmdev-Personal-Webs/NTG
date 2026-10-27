package com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity;

import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A randomlocke: a rule set played by a fixed group of players, with one or more tournaments. Its
 * tournaments can be played in different games (a generational locke moves from game to game).
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lockes", schema = "pokemon")
public class Locke {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Free-form rules document (see the frontend LockeRules model). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode rules;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Fixed roster. New tournaments of the locke start with these players. */
    @ManyToMany
    @JoinTable(
            name = "locke_players",
            schema = "pokemon",
            joinColumns = @JoinColumn(name = "locke_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id"))
    private Set<Player> roster = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
