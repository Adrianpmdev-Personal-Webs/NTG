package com.adrianpmdev.wowforever.features.characters.infrastructure.armory;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What is read from the pages of the armory, on pages shaped like the real ones with made-up characters.
 *
 * @author adrianpmdev
 */
class ArmoryPageTest {

    private static final String SEARCH = page("""
            {"categories":[
              {"enum":"CHARACTER","results":[
                {"name":"Experto","surname":"Uno","slug":"experto-uno","lastUpdatedTimestamp":{"epoch":1000}},
                {"name":"Uno","surname":"Experto","slug":"uno-experto","lastUpdatedTimestamp":{"epoch":2000}},
                {"name":"Solo","slug":"solo"}]},
              {"enum":"GUILD","results":[{"name":"Dos","surname":"Experto","slug":"una-hermandad"}]}]}""");

    private static final String CHARACTER = page("""
            {"reactMounts":[{"initialState":{"character":{
              "slug":"uno-experto","name":"Uno","surname":"Experto","level":13,
              "class":{"name":"Rogue"},"race":{"name":"Night Elf"},
              "lastUpdatedTimestamp":{"epoch":2000},
              "professions":{
                "primaries":[
                  {"name":"Mining","tiers":[{"skillPoints":70,"maxSkillPoints":150}]},
                  {"name":"Engineering","tiers":[
                    {"skillPoints":10,"maxSkillPoints":75},{"skillPoints":75,"maxSkillPoints":150}]},
                  {"name":"Without tiers","tiers":[]}],
                "secondaries":[{"name":"First Aid","tiers":[{"skillPoints":79,"maxSkillPoints":150}]}]}
            }}}]}""");

    @Test
    void theSearchOnlyAcceptsTheExactNameInAnyCase() throws IOException {
        assertThat(ArmoryPage.match(SEARCH, "uno EXPERTO")).contains(
                new ArmoryMatch("uno-experto", Instant.ofEpochMilli(2000)));
        assertThat(ArmoryPage.match(SEARCH, "Experto")).isEmpty();
    }

    @Test
    void aCharacterWithoutSurnameIsFoundAndMayNotTellItsDate() throws IOException {
        assertThat(ArmoryPage.match(SEARCH, "Solo")).contains(new ArmoryMatch("solo", null));
    }

    @Test
    void aGuildIsNotACharacter() throws IOException {
        assertThat(ArmoryPage.match(SEARCH, "Dos Experto")).isEmpty();
    }

    @Test
    void aSearchWithoutResultsFindsNothing() throws IOException {
        assertThat(ArmoryPage.match(page("{\"categories\":[{\"enum\":\"CHARACTER\",\"results\":[]}]}"), "Nadie")).isEmpty();
    }

    @Test
    void aCharacterPageGivesItsLevelAndProfessions() throws IOException {
        assertThat(ArmoryPage.character(CHARACTER)).isEqualTo(new ArmoryCharacter("uno-experto", "Uno Experto", "Rogue",
                "Night Elf", 13, List.of(
                        new ArmoryCharacter.Profession("Mining", 70, 150),
                        new ArmoryCharacter.Profession("Engineering", 75, 150),
                        new ArmoryCharacter.Profession("First Aid", 79, 150)),
                Instant.ofEpochMilli(2000)));
    }

    @Test
    void aPageThatChangedItsFormatIsAnErrorInsteadOfEmptyData() {
        assertThatThrownBy(() -> ArmoryPage.match("<html></html>", "Uno")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> ArmoryPage.match(page("{\"other\":1}"), "Uno")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> ArmoryPage.character(page("{\"layout\":true}"))).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> ArmoryPage.character(page("{not json"))).isInstanceOf(IOException.class);
    }

    private static String page(String model) {
        return "<html><head><script src=\"a.js\"></script><script id=\"model\">model = " + model + ";</script></head></html>";
    }
}
