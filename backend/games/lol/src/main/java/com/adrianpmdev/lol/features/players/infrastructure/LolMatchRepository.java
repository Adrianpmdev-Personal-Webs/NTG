package com.adrianpmdev.lol.features.players.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Games of the players. History and statistics are always read from here, never from Riot.
 *
 * @author adrianpmdev
 */
public interface LolMatchRepository extends JpaRepository<LolMatch, LolMatch.Key> {

    /**
     * What a player did with a champion in a queue.
     */
    interface ChampionTotals {

        int getQueueId();

        int getChampionId();

        String getChampionName();

        long getGames();

        long getWins();

        long getKills();

        long getDeaths();

        long getAssists();

        long getCs();

        long getSeconds();
    }

    /**
     * The newest games of a player in a queue.
     *
     * @param userId  player
     * @param queueId queue
     * @return up to 20 games
     */
    List<LolMatch> findTop20ByUserIdAndQueueIdOrderByPlayedAtDesc(UUID userId, int queueId);

    /**
     * Totals of a player by queue and champion since a moment, without remakes.
     *
     * @param userId player
     * @param from   start of the season
     * @return totals
     */
    @Query("""
            select m.queueId as queueId, m.championId as championId, max(m.championName) as championName,
                   count(m.matchId) as games, sum(case when m.win = true then 1L else 0L end) as wins,
                   sum(m.kills) as kills, sum(m.deaths) as deaths, sum(m.assists) as assists,
                   sum(m.cs) as cs, sum(m.durationSeconds) as seconds
            from LolMatch m
            where m.userId = :userId and m.remake = false and m.playedAt >= :from
            group by m.queueId, m.championId
            """)
    List<ChampionTotals> championTotals(@Param("userId") UUID userId, @Param("from") Instant from);

    /**
     * Which of some games are already stored for a player.
     *
     * @param userId   player
     * @param matchIds match ids
     * @return the ones stored
     */
    @Query("select m.matchId from LolMatch m where m.userId = :userId and m.matchId in :matchIds")
    List<String> findStored(@Param("userId") UUID userId, @Param("matchIds") Collection<String> matchIds);

    /**
     * How many games of a player are stored.
     *
     * @param userId player
     * @return number of games
     */
    long countByUserId(UUID userId);
}
