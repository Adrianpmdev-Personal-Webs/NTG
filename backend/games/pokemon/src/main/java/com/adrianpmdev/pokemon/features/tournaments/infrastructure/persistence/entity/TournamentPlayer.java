package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity;

import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
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

import java.util.UUID;

/**
 * A player inside one tournament: group, team picture and points adjustment.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tournament_players", schema = "pokemon")
public class TournamentPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /** A or B in a groups_bracket tournament; null in a league or for the player that skips the groups. */
    @Column(name = "group_name", length = 1)
    private String groupName;

    /** Object name in the private bucket; never sent to clients (they get a signed URL). */
    @Column(name = "team_image", length = 200)
    private String teamImage;

    /**
     * Points added to (or taken from) the series wins in the standings. Used to keep the imported
     * history, and by admins for bonuses or penalties.
     */
    @Column(name = "points_adjustment", nullable = false)
    private short pointsAdjustment;
}
