package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The game data responses built from a small data file cut from a real export (test resource
 * {@code wow-forever/game-data.json}) and the real config.json: what every screen relies on.
 *
 * @author adrianpmdev
 */
class GameDataServiceTest {

    private static final String ICON_PREFIX = "https://render.worldofwarcraft.com/us/icons/56/";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GameDataService service = GameDataFixtures.published();

    @Test
    void nothingIsServedBeforeTheFirstFileButTheNewsSettingsAre() {
        GameDataService empty = new GameDataService();

        assertThat(empty.currentBuild()).isEmpty();
        assertThatThrownBy(() -> empty.getTalents("warrior"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(empty.getNewsSettings().feedUrl()).startsWith("https://");
    }

    @Test
    void indexHasClassesToolsAndPointLevels() throws Exception {
        JsonNode index = read(service.getIndex());

        assertThat(service.currentBuild()).contains(index.path("credit").path("build").asText());
        assertThat(index.path("credit").path("build").asText()).matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
        assertThat(index.path("classes")).hasSize(1);
        assertThat(index.path("classes").path(0).path("icon").asText()).startsWith(ICON_PREFIX).endsWith(".jpg");
        assertThat(index.path("features").isArray()).isTrue();
        assertThat(index.path("factions").path("HORDE").asText()).isEqualTo("Horda");
        assertThat(index.path("talentPointLevels")).hasSize(51);
        assertThat(index.path("talentPointLevels").path(0).asInt()).isEqualTo(10);
    }

    @Test
    void talentTreesUseGameIdsAndValidPrerequisites() throws Exception {
        JsonNode trees = read(service.getTalents("warrior")).path("trees");

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
    void spellbookHasTheTabsOfTheTalentTrees() throws Exception {
        JsonNode spellbook = read(service.getSpellbook("warrior"));

        assertThat(spellbook.path("tabs").findValuesAsText("name")).startsWith("Arms");
        assertThat(spellbook.path("tabs").path(0).path("spells").path(0).path("ranks").size()).isPositive();
    }

    @Test
    void spellbookTabsShowTheIconOfTheirTalentTree() throws Exception {
        JsonNode spellbook = read(service.getSpellbook("warrior"));
        JsonNode talents = read(service.getTalents("warrior"));

        for (int index = 0; index < 3; index++) {
            assertThat(spellbook.path("tabs").path(index).path("icon").asText())
                    .isEqualTo(talents.path("trees").path(index).path("icon").asText())
                    .startsWith(ICON_PREFIX);
        }
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
        assertThat(racials.path("races").path(0).path("icon").asText())
                .startsWith("https://wow.zamimg.com/images/wow/icons/large/").endsWith(".jpg");
        assertThat(racials.path("races").path(0).path("abilities").path(0).path("icon").asText())
                .startsWith(ICON_PREFIX);
    }

    @Test
    void unknownClassesAnswerNotFound() {
        assertThatThrownBy(() -> service.getTalents("monk"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void aNewFileReplacesTheDataAndItsResponses() throws Exception {
        assertThat(read(service.getIndex()).path("credit").path("build").asText()).isNotEqualTo("9.9.9.9");
        ObjectNode newer = GameDataFixtures.testData();
        newer.put("build", "9.9.9.9");
        ((ObjectNode) newer.path("talents").path("Warrior")).put("source", "WoW Forever client, build 9.9.9.9");

        service.publish(newer);

        assertThat(service.currentBuild()).contains("9.9.9.9");
        assertThat(read(service.getIndex()).path("credit").path("build").asText()).isEqualTo("9.9.9.9");
    }

    @Test
    void incompleteFilesAreRefusedAndTheServedOneStays() {
        ObjectNode noLegacy = GameDataFixtures.testData();
        noLegacy.remove("legacy");
        ObjectNode noHorde = GameDataFixtures.testData();
        ((ObjectNode) noHorde.path("racials")).putArray("Horde");

        assertThatThrownBy(() -> service.publish(noLegacy)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legacy");
        assertThatThrownBy(() -> service.publish(noHorde)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Horde");
        assertThat(service.currentBuild()).contains(GameDataFixtures.testData().path("build").asText());
    }

    private static JsonNode read(String json) throws Exception {
        return OBJECT_MAPPER.readTree(json);
    }
}
