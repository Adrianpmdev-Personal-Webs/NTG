package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import com.adrianpmdev.pokemon.features.tournaments.domain.MatchStage;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Series and their games.
 *
 * @author adrianpmdev
 */
public interface MatchRepository extends JpaRepository<Match, UUID> {

    /**
     * Series of a tournament with their games.
     *
     * @param tournamentId tournament id
     * @return series
     */
    @Query("select distinct m from Match m left join fetch m.games where m.tournament.id = :tournamentId")
    List<Match> findByTournament(@Param("tournamentId") UUID tournamentId);

    /**
     * Series of every tournament of a randomlocke, with their games.
     *
     * @param lockeId locke id
     * @return series
     */
    @Query("select distinct m from Match m left join fetch m.games join fetch m.tournament t where t.locke.id = :lockeId")
    List<Match> findByLocke(@Param("lockeId") UUID lockeId);

    /**
     * Every series with its games.
     *
     * @return series
     */
    @Query("select distinct m from Match m left join fetch m.games join fetch m.tournament")
    List<Match> findAllWithGames();

    /**
     * Whether any series of the given stages already has games.
     *
     * @param tournamentId tournament id
     * @param stages       stages to check
     * @return true when some game was recorded
     */
    @Query("select count(g) > 0 from MatchGame g where g.match.tournament.id = :tournamentId and g.match.stage in :stages")
    boolean hasGames(@Param("tournamentId") UUID tournamentId, @Param("stages") Collection<MatchStage> stages);

    /**
     * Whether a tournament player plays a series that already has games.
     *
     * @param tournamentPlayerId tournament player id
     * @return true when they have recorded games
     */
    @Query("select count(g) > 0 from MatchGame g where g.match.player1.id = :id or g.match.player2.id = :id")
    boolean playerHasGames(@Param("id") UUID tournamentPlayerId);
}
