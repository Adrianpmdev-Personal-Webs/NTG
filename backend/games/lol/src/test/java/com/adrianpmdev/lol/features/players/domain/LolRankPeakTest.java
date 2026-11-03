package com.adrianpmdev.lol.features.players.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The highest rank goes by tier, then division, then LP, in each queue on its own.
 *
 * @author adrianpmdev
 */
class LolRankPeakTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode ranks(String queue, String tier, String division, int lp) throws JsonProcessingException {
        return JSON.readTree("[{\"queueType\":\"%s\",\"tier\":\"%s\",\"rank\":\"%s\",\"leaguePoints\":%d}]"
                .formatted(queue, tier, division, lp));
    }

    private static String only(JsonNode peak) {
        JsonNode rank = peak.get(0);

        return rank.path("tier").asText() + " " + rank.path("rank").asText() + " " + rank.path("leaguePoints").asInt();
    }

    @Test
    void aHigherTierWinsWhateverItsDivisionAndLp() throws JsonProcessingException {
        JsonNode peak = LolRankPeak.best(ranks("SOLO", "GOLD", "I", 99), ranks("SOLO", "PLATINUM", "IV", 0));

        assertThat(only(peak)).isEqualTo("PLATINUM IV 0");
    }

    @Test
    void inOneTierADivisionWinsOverLp() throws JsonProcessingException {
        JsonNode peak = LolRankPeak.best(ranks("SOLO", "GOLD", "II", 99), ranks("SOLO", "GOLD", "I", 0));

        assertThat(only(peak)).isEqualTo("GOLD I 0");
    }

    @Test
    void inOneDivisionTheLpDecide() throws JsonProcessingException {
        JsonNode peak = LolRankPeak.best(ranks("SOLO", "GOLD", "II", 40), ranks("SOLO", "GOLD", "II", 12));

        assertThat(only(peak)).isEqualTo("GOLD II 40");
    }

    @Test
    void theApexTiersGoByTheirLp() throws JsonProcessingException {
        JsonNode peak = LolRankPeak.best(ranks("SOLO", "MASTER", "I", 900), ranks("SOLO", "GRANDMASTER", "I", 300));

        assertThat(only(peak)).isEqualTo("GRANDMASTER I 300");
    }

    @Test
    void droppingDoesNotLowerThePeak() throws JsonProcessingException {
        JsonNode peak = LolRankPeak.best(ranks("SOLO", "DIAMOND", "III", 45), ranks("SOLO", "EMERALD", "I", 80));

        assertThat(only(peak)).isEqualTo("DIAMOND III 45");
    }

    @Test
    void eachQueueKeepsItsOwn() throws JsonProcessingException {
        JsonNode peak = LolRankPeak.best(ranks("SOLO", "GOLD", "I", 0), ranks("FLEX", "SILVER", "I", 0));

        assertThat(peak).hasSize(2);
    }

    @Test
    void withNothingKnownThereIsNoPeak() {
        assertThat(LolRankPeak.best(JSON.createArrayNode(), JSON.createArrayNode())).isEmpty();
    }
}
