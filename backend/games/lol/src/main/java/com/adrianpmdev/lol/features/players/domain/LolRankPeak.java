package com.adrianpmdev.lol.features.players.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The highest rank seen of a player, by queue, among ranks as Riot gives them. Riot does not tell the highest rank of a
 * season, so this only knows the ranks read when the player is updated.
 *
 * @author adrianpmdev
 */
public final class LolRankPeak {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> TIERS = List.of("IRON", "BRONZE", "SILVER", "GOLD", "PLATINUM", "EMERALD", "DIAMOND",
            "MASTER", "GRANDMASTER", "CHALLENGER");
    private static final List<String> DIVISIONS = List.of("IV", "III", "II", "I");
    /** More than the LP of any tier, so a division always weighs more than the LP inside it. */
    private static final int DIVISION_WEIGHT = 10_000;

    private LolRankPeak() {
    }

    /**
     * The highest rank of each queue.
     *
     * @param lists lists of ranks as Riot gives them
     * @return one rank per queue, the highest of all the lists
     */
    public static JsonNode best(JsonNode... lists) {
        Map<String, JsonNode> best = new LinkedHashMap<>();

        for (JsonNode list : lists) {
            for (JsonNode rank : list) {
                best.merge(rank.path("queueType").asText(), rank,
                        (kept, candidate) -> score(candidate) > score(kept) ? candidate : kept);
            }
        }

        ArrayNode result = JSON.createArrayNode();
        best.values().forEach(result::add);

        return result;
    }

    private static long score(JsonNode rank) {
        long tier = TIERS.indexOf(rank.path("tier").asText());
        long division = DIVISIONS.indexOf(rank.path("rank").asText());

        return (tier * DIVISIONS.size() + division) * DIVISION_WEIGHT + rank.path("leaguePoints").asInt();
    }
}
