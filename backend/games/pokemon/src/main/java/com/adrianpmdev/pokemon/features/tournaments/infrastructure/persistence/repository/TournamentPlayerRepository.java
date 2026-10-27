package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Players inside tournaments.
 *
 * @author adrianpmdev
 */
public interface TournamentPlayerRepository extends JpaRepository<TournamentPlayer, UUID> {

    /**
     * Players of a tournament with their player row.
     *
     * @param tournamentId tournament id
     * @return tournament players
     */
    @Query("select tp from TournamentPlayer tp join fetch tp.player where tp.tournament.id = :tournamentId")
    List<TournamentPlayer> findByTournament(@Param("tournamentId") UUID tournamentId);

    /**
     * Players of every tournament of a randomlocke, with their player and tournament.
     *
     * @param lockeId locke id
     * @return tournament players
     */
    @Query("select tp from TournamentPlayer tp join fetch tp.player join fetch tp.tournament t where t.locke.id = :lockeId")
    List<TournamentPlayer> findByLocke(@Param("lockeId") UUID lockeId);

    /**
     * Players of every tournament, with their player and tournament.
     *
     * @return tournament players
     */
    @Query("select tp from TournamentPlayer tp join fetch tp.player join fetch tp.tournament")
    List<TournamentPlayer> findAllWithPlayerAndTournament();

    /**
     * One tournament player with its player and tournament.
     *
     * @param id tournament player id
     * @return the tournament player
     */
    @Query("select tp from TournamentPlayer tp join fetch tp.player join fetch tp.tournament where tp.id = :id")
    Optional<TournamentPlayer> findDetailedById(@Param("id") UUID id);

    /**
     * Whether a player is already in a tournament.
     *
     * @param tournamentId tournament id
     * @param playerId     player id
     * @return true when the player is in it
     */
    boolean existsByTournamentIdAndPlayerId(UUID tournamentId, UUID playerId);

    /**
     * Team pictures stored for a set of tournaments (to delete them with the tournaments).
     *
     * @param tournamentIds tournament ids
     * @return object names
     */
    @Query("select tp.teamImage from TournamentPlayer tp where tp.tournament.id in :tournamentIds and tp.teamImage is not null")
    List<String> findTeamImages(@Param("tournamentIds") List<UUID> tournamentIds);
}
