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

import java.util.UUID;

/**
 * A reward of a tournament. Players pick one each, in the order of the final standings of the
 * tournament; an admin records the pick.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rewards", schema = "pokemon")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(length = 150, nullable = false)
    private String name;

    /** Tournament player who picked it; null while nobody has. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_by")
    private TournamentPlayer chosenBy;
}
