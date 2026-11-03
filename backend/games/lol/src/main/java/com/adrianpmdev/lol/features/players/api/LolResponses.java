package com.adrianpmdev.lol.features.players.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Answers of the League of Legends API. Names and icon URLs come ready to show.
 *
 * @author adrianpmdev
 */
public final class LolResponses {

    private LolResponses() {
    }

    /**
     * The players with a linked account.
     *
     * @param myUserId id of the caller
     * @param myRiotId Riot ID the caller linked, or null
     * @param players  the players, sorted by name
     */
    public record Players(UUID myUserId, String myRiotId, List<PlayerSummary> players) {
    }

    /**
     * A player in the list.
     *
     * @param userId   platform user id
     * @param username name of the platform account, or null when it no longer exists
     * @param riotId   Riot ID
     * @param state    PENDING, QUEUED, PREPARING, LOADING, COMPLETE, ERROR or INTERRUPTED
     * @param iconUrl  profile icon, or null until the first update
     * @param level    summoner level, or null until the first update
     */
    public record PlayerSummary(UUID userId, String username, String riotId, String state, String iconUrl,
                                Integer level) {
    }

    /**
     * Everything shown about a player. It is sent whole, so moving between queues costs no call.
     *
     * @param player    the player
     * @param checkedAt when Riot was last asked, or null
     * @param updating  whether new games are still on their way
     * @param error     why the last update failed, or null
     * @param season    the season shown
     * @param current   whether it is the season being played
     * @param seasons   the seasons of this player, newest first
     * @param ranks     official ranks in that season; for a past one, the last known
     * @param queues    one block per queue, of that season
     */
    public record PlayerDetail(PlayerSummary player, Instant checkedAt, boolean updating, String error,
                               SeasonOption season, boolean current, List<SeasonOption> seasons, List<Rank> ranks,
                               List<QueueBlock> queues) {
    }

    /**
     * A season in the admin screen.
     *
     * @param key     short name
     * @param label   name shown
     * @param date    first day, as yyyy-MM-dd
     * @param current whether it is the season being played
     */
    public record AdminSeason(String key, String label, String date, boolean current) {
    }

    /**
     * A season to choose.
     *
     * @param key   short name
     * @param label name shown
     */
    public record SeasonOption(String key, String label) {
    }

    /**
     * An official rank.
     *
     * @param queue  name of the queue
     * @param tier   tier and division, in Spanish
     * @param lp     LP
     * @param wins   wins
     * @param losses losses
     * @param iconUrl URL of the emblem of the tier, or null while it is not uploaded
     * @param peak    highest rank seen in that season, or null when none was
     */
    public record Rank(String queue, String tier, int lp, int wins, int losses, String iconUrl, PeakRank peak) {
    }

    /**
     * The highest rank seen of a player in a queue.
     *
     * @param tier tier and division, in Spanish
     * @param lp   LP
     */
    public record PeakRank(String tier, int lp) {
    }

    /**
     * A queue of a player.
     *
     * @param id        queue id
     * @param label     name of the queue
     * @param emptyNotice what to tell while there are no games, or null for the usual text
     * @param stats     totals of the season
     * @param champions most played champions of the season
     * @param matches   last games
     */
    public record QueueBlock(int id, String label, String emptyNotice, Stats stats, List<ChampionStats> champions,
                             List<Match> matches) {
    }

    /**
     * Totals of the season in a queue, without remakes.
     *
     * @param games      games
     * @param wins       wins
     * @param winRate    percentage of wins
     * @param kda        (kills + assists) / deaths
     * @param kills      kills per game
     * @param deaths     deaths per game
     * @param assists    assists per game
     * @param csPerMinute minions and monsters per minute
     */
    public record Stats(long games, long wins, int winRate, double kda, double kills, double deaths, double assists,
                        double csPerMinute) {
    }

    /**
     * A champion of the season in a queue.
     *
     * @param name    name
     * @param icon    icon URL, or null
     * @param games   games
     * @param winRate percentage of wins
     * @param kda     (kills + assists) / deaths
     */
    public record ChampionStats(String name, String icon, long games, int winRate, double kda) {
    }

    /**
     * A game of a player.
     *
     * @param id              match id
     * @param playedAt        when it was played
     * @param durationSeconds duration
     * @param champion        name of the champion
     * @param championIcon    icon URL, or null
     * @param win             whether the player won
     * @param remake          whether it ended by an early surrender
     * @param kills           kills
     * @param deaths          deaths
     * @param assists         assists
     * @param cs              minions and monsters
     * @param damage          damage to champions
     * @param gold            gold
     * @param vision          vision score
     * @param level           final level
     * @param items           icon URLs of the items
     * @param spells          icon URLs of the summoner spells
     */
    public record Match(String id, Instant playedAt, int durationSeconds, String champion, String championIcon,
                        boolean win, boolean remake, int kills, int deaths, int assists, int cs, int damage, int gold,
                        int vision, int level, List<String> items, List<String> spells) {
    }

    /**
     * An account in the admin screen.
     *
     * @param userId   platform user id
     * @param username name of the platform account
     * @param riotId   Riot ID, or null when it is not linked
     * @param state    NOT_LINKED, PENDING, QUEUED, PREPARING, LOADING, COMPLETE, ERROR or INTERRUPTED
     * @param stored   games stored
     * @param saved    games stored in the running download, or null
     * @param total    games of the running download, or null
     * @param secondsLeft rough time left of the running download, or null
     * @param error    why the download failed, or null
     */
    public record AdminAccount(UUID userId, String username, String riotId, String state, long stored, Integer saved,
                               Integer total, Integer secondsLeft, String error) {
    }
}
