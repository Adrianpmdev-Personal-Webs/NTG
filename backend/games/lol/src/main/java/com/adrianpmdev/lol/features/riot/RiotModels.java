package com.adrianpmdev.lol.features.riot;

import java.util.List;

/**
 * The fields of the Riot API this module uses; everything else in an answer is skipped while reading it.
 *
 * @author adrianpmdev
 */
public final class RiotModels {

    private RiotModels() {
    }

    /**
     * A Riot account.
     *
     * @param puuid    player id
     * @param gameName name of the Riot ID
     * @param tagLine  tag of the Riot ID
     */
    public record Account(String puuid, String gameName, String tagLine) {
    }

    /**
     * An official rank.
     *
     * @param queueType    queue, such as RANKED_SOLO_5x5
     * @param tier         tier, such as GOLD
     * @param rank         division, such as II
     * @param leaguePoints LP
     * @param wins         wins of the season
     * @param losses       losses of the season
     */
    public record Rank(String queueType, String tier, String rank, int leaguePoints, int wins, int losses) {
    }

    /**
     * A game.
     *
     * @param info what happened in it
     */
    public record Match(Info info) {
    }

    /**
     * What happened in a game.
     *
     * @param queueId      queue
     * @param gameCreation when it was created, in epoch milliseconds
     * @param gameDuration duration in seconds
     * @param participants its players
     */
    public record Info(int queueId, long gameCreation, long gameDuration, List<Participant> participants) {
    }

    /**
     * A player in a game.
     *
     * @param puuid                       player id
     * @param championId                  champion
     * @param championName                internal name of the champion
     * @param win                         whether their team won
     * @param gameEndedInEarlySurrender   whether the game was a remake
     * @param kills                       kills
     * @param deaths                      deaths
     * @param assists                     assists
     * @param totalMinionsKilled          minions
     * @param neutralMinionsKilled        jungle monsters
     * @param totalDamageDealtToChampions damage to champions
     * @param goldEarned                  gold
     * @param visionScore                 vision score
     * @param champLevel                  final level
     * @param item0                       first slot
     * @param item1                       second slot
     * @param item2                       third slot
     * @param item3                       fourth slot
     * @param item4                       fifth slot
     * @param item5                       sixth slot
     * @param item6                       trinket
     * @param summoner1Id                 first summoner spell
     * @param summoner2Id                 second summoner spell
     */
    public record Participant(String puuid, int championId, String championName, boolean win,
                              boolean gameEndedInEarlySurrender, int kills, int deaths, int assists,
                              int totalMinionsKilled, int neutralMinionsKilled, int totalDamageDealtToChampions,
                              int goldEarned, int visionScore, int champLevel, int item0, int item1, int item2,
                              int item3, int item4, int item5, int item6, int summoner1Id, int summoner2Id) {
    }
}
