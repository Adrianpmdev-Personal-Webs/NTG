package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The game data responses built from the real data.json and config.json: what every screen relies on.
 *
 * @author adrianpmdev
 */
class GameDataServiceTest {

    private static final String ICON_PREFIX = "https://render.worldofwarcraft.com/us/icons/56/";

    private final GameDataService service = new GameDataService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void indexHasClassesToolsAndPointLevels() throws Exception {
        JsonNode index = read(service.getIndex());

        assertThat(index.path("classes")).hasSize(9);
        assertThat(index.path("classes").path(0).path("icon").asText()).startsWith(ICON_PREFIX).endsWith(".jpg");
        assertThat(index.path("features").isArray()).isTrue();
        assertThat(index.path("factions").path("HORDE").asText()).isEqualTo("Horda");
        assertThat(index.path("talentPointLevels")).hasSize(51);
        assertThat(index.path("talentPointLevels").path(0).asInt()).isEqualTo(10);
    }

    @Test
    void talentTreesUseGameIdsAndValidPrerequisites() throws Exception {
        JsonNode talents = read(service.getTalents("warrior"));
        JsonNode trees = talents.path("trees");

        assertThat(trees).hasSize(3);
        assertThat(trees.path(0).path("id").asInt()).isEqualTo(26);
        assertThat(trees.path(0).path("gateScope").asText()).isEqualTo("rows-above");

        for (JsonNode tree : trees) {
            Set<Integer> ids = new HashSet<>();
            tree.path("talents").forEach(talent -> ids.add(talent.path("id").asInt()));

            for (JsonNode talent : tree.path("talents")) {
                assertThat(talent.path("icon").asText()).startsWith(ICON_PREFIX);
                assertThat(talent.path("pointsRequired").asInt()).isEqualTo(talent.path("row").asInt() * 5);
                talent.path("requires").forEach(required -> assertThat(ids).contains(required.asInt()));
            }
        }
    }

    @Test
    void spellbookStartsWithTheGeneralTab() throws Exception {
        JsonNode spellbook = read(service.getSpellbook("mage"));

        assertThat(spellbook.path("tabs").path(0).path("name").asText()).isEqualTo("General");
        assertThat(spellbook.path("tabs").path(1).path("spells").path(0).path("ranks").size()).isPositive();
    }

    @Test
    void legacyGroupsAreInSpanish() throws Exception {
        JsonNode legacy = read(service.getLegacy());

        assertThat(legacy.path("challengeGroups").findValuesAsText("name")).contains("Clases", "Profesiones");
        assertThat(legacy.path("trees").path(0).path("gateScope").asText()).isEqualTo("tree");
    }

    @Test
    void racialsCoverBothFactions() throws Exception {
        JsonNode racials = read(service.getRacials());

        assertThat(racials.path("races").findValuesAsText("faction")).contains("ALLIANCE", "HORDE");
    }

    private JsonNode read(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
