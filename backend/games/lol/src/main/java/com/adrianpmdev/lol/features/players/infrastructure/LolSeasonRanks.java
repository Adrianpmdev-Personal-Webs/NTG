package com.adrianpmdev.lol.features.players.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The last official rank known of each player in each season. Riot only tells the current rank, so it is copied every
 * time a player is updated: what remains when the season changes is the rank they finished with.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class LolSeasonRanks {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    /**
     * Keeps the rank of a player in a season, if they still have the account it was read from.
     *
     * @param userId player
     * @param puuid  account the rank was read from
     * @param season short name of the season
     * @param ranks  official ranks
     */
    public void save(UUID userId, String puuid, String season, JsonNode ranks) {
        jdbcTemplate.update("""
                insert into lol.season_ranks (user_id, season, ranks, updated_at)
                select ?::uuid, ?, ?::jsonb, now()
                where exists (select 1 from lol.accounts where user_id = ?::uuid and puuid = ?)
                on conflict (user_id, season) do update set ranks = excluded.ranks, updated_at = now()
                """, userId, season, ranks.toString(), userId, puuid);
    }

    /**
     * The last rank known of a player in a season.
     *
     * @param userId player
     * @param season short name of the season
     * @return official ranks; empty when none was kept
     */
    public JsonNode find(UUID userId, String season) {
        List<String> found = jdbcTemplate.queryForList(
                "select ranks::text from lol.season_ranks where user_id = ? and season = ?", String.class, userId,
                season);

        if (found.isEmpty()) {
            return JSON.createArrayNode();
        }

        try {
            return JSON.readTree(found.getFirst());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The rank of a season could not be read", exception);
        }
    }
}
