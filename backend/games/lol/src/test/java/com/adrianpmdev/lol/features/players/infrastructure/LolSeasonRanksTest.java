package com.adrianpmdev.lol.features.players.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A rank is only written when it is not the one already kept.
 *
 * @author adrianpmdev
 */
class LolSeasonRanksTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final String KEPT = """
            [{"tier": "GOLD", "wins": 10, "losses": 5}]""";

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final LolSeasonRanks ranks = new LolSeasonRanks(jdbcTemplate);

    @Test
    void theSameRankWritesNothing() throws Exception {
        keeps(KEPT);

        ranks.save(PLAYER, "puuid", "S1", rank("""
                [{"wins":10,"losses":5,"tier":"GOLD"}]"""));

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void anotherRankIsWritten() throws Exception {
        keeps(KEPT);

        ranks.save(PLAYER, "puuid", "S1", rank("""
                [{"wins":11,"losses":5,"tier":"GOLD"}]"""));

        verify(jdbcTemplate, times(1)).update(anyString(), any(Object[].class));
    }

    private void keeps(String kept) {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(), any())).thenReturn(List.of(kept));
    }

    private static JsonNode rank(String json) throws Exception {
        return new ObjectMapper().readTree(json);
    }
}
