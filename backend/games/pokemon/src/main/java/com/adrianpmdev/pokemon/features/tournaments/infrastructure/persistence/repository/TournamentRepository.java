package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tournaments.
 *
 * @author adrianpmdev
 */
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    /**
     * One tournament with its randomlocke and game.
     *
     * @param id tournament id
     * @return the tournament
     */
    @EntityGraph(attributePaths = {"locke", "game"})
    Optional<Tournament> findWithLockeAndGameById(UUID id);

    /**
     * Tournaments of a randomlocke in play order, with their game.
     *
     * @param lockeId locke id
     * @return tournaments
     */
    @EntityGraph(attributePaths = "game")
    List<Tournament> findByLockeIdOrderByPositionAsc(UUID lockeId);

    /**
     * Every tournament with its randomlocke and game.
     *
     * @return tournaments
     */
    @EntityGraph(attributePaths = {"locke", "game"})
    List<Tournament> findAllByOrderByPositionAsc();

    /**
     * Ids of the tournaments of a randomlocke (without loading them).
     *
     * @param lockeId locke id
     * @return tournament ids
     */
    @Query("select t.id from Tournament t where t.locke.id = :lockeId")
    List<UUID> findIdsByLocke(@Param("lockeId") UUID lockeId);

    /** Number of randomlockes played in a game. */
    interface GameLockeCount {

        UUID getGameId();

        long getLockes();
    }

    /**
     * Number of randomlockes with tournaments in each game.
     *
     * @return one row per game that has tournaments
     */
    @Query("select t.game.id as gameId, count(distinct t.locke.id) as lockes from Tournament t group by t.game.id")
    List<GameLockeCount> countLockesByGame();

    /**
     * Last position used in a randomlocke.
     *
     * @param lockeId locke id
     * @return highest position, 0 when it has no tournaments
     */
    @Query("select coalesce(max(t.position), 0) from Tournament t where t.locke.id = :lockeId")
    int findLastPosition(@Param("lockeId") UUID lockeId);
}
