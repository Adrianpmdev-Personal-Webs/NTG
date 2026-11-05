package com.adrianpmdev.wowforever.features.characters.infrastructure.armory;

import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;
import com.fasterxml.jackson.databind.ObjectMapper;
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
              "slug":"uno-experto","name":"Uno","surname":"Experto","level":13,"averageItemLevel":12,
              "class":{"name":"Rogue"},"race":{"name":"Night Elf"},"faction":{"enum":"ALLIANCE"},
              "gender":{"name":"Male"},"guild":{"name":"Los Expertos"},
              "lastUpdatedTimestamp":{"epoch":2000},
              "media":{"avatar":{"url":"avatar.jpg"},"main-raw":{"url":"main.png"}},
              "render":{"background":{"url":"bg.jpg"},"foreground":{"url":"render.png"}},
              "pvp":{"honorableKills":{"value":3}},
              "professions":{
                "primaries":[
                  {"name":"Mining","icon":{"url":"mining.jpg"},"tiers":[{"skillPoints":70,"maxSkillPoints":150}]},
                  {"name":"Engineering","tiers":[
                    {"skillPoints":10,"maxSkillPoints":75},{"skillPoints":75,"maxSkillPoints":150}]},
                  {"name":"Without tiers","tiers":[]}],
                "secondaries":[{"name":"First Aid","tiers":[{"skillPoints":79,"maxSkillPoints":150}]}]},
              "gear":{"weapon":{
                "slot":{"type":"MAIN_HAND","name":"Main Hand"},"name":"Blade of Cunning","quality":{"type":"UNCOMMON"},
                "media":{"content":{"assets":[{"key":"icon","value":"blade.jpg"}]}},
                "level":{"display_string":"Item Level 13"},"binding":{"name":"Binds when picked up"},
                "inventory_type":{"name":"One-Hand"},"item_subclass":{"name":"Dagger"},
                "weapon":{"damage":{"display_string":"9 - 18 Damage"},"attack_speed":{"display_string":"Speed 2.00"},
                          "dps":{"display_string":"(6.8 damage per second)"}},
                "stats":[{"display":{"display_string":"+2 Agility","color":{"r":255,"g":255,"b":255}}}],
                "durability":{"display_string":"Durability 30 / 30"}},
               "chest":{"slot":{"type":"CHEST","name":"Chest"},"name":"Tunic","quality":{"type":"COMMON"},
                "armor":{"value":62}}},
              "stats":{"groups":[{"name":"Attack","stats":[
                {"enum":"DAMAGE","value":{"type":"RANGE","min":{"value":20.43},"max":{"value":29.43}}},
                {"enum":"CRITICALSTRIKE","value":{"type":"PERCENTAGE","value":5.256}},
                {"enum":"ATTACKSPEED","value":{"type":"DECIMAL","value":1.7}},
                {"enum":"NEWSTAT","value":{"type":"WHOLE","value":80}}]}]},
              "specs":[{"name":"Assassination","icon":{"url":"a.jpg"},"spentPoints":2,"talents":[
                {"name":"Relentless Strikes","rank":2,"icon":{"url":"r.jpg"},"cast":"Passive","description":"Hits."}]}]
            }}}]}""");

    @Test
    void theSearchOnlyAcceptsTheExactNameInAnyCase() throws IOException {
        assertThat(ArmoryPage.match(SEARCH, "uno EXPERTO")).contains(
                new ArmoryMatch("uno-experto", Instant.ofEpochMilli(2000)));
        assertThat(ArmoryPage.match(SEARCH, "Experto")).isEmpty();
        assertThat(ArmoryPage.match(SEARCH, "Solo")).contains(new ArmoryMatch("solo", null));
        assertThat(ArmoryPage.match(SEARCH, "Dos Experto")).isEmpty();
    }

    @Test
    void theSheetHasTheCharacterWithTheSlugsOfTheGameData() throws IOException {
        ArmorySheet sheet = ArmoryPage.sheet(CHARACTER);

        assertThat(sheet.name()).isEqualTo("Uno Experto");
        assertThat(sheet.level()).isEqualTo(13);
        assertThat(sheet.classSlug()).isEqualTo("rogue");
        assertThat(sheet.raceSlug()).isEqualTo("night-elf");
        assertThat(sheet.faction()).isEqualTo("ALLIANCE");
        assertThat(sheet.guild()).isEqualTo("Los Expertos");
        assertThat(sheet.honorableKills()).isEqualTo(3);
        assertThat(sheet.images()).isEqualTo(new ArmorySheet.Images("avatar.jpg", "render.png", "bg.jpg"));
    }

    @Test
    void professionsTellWhatTheirHighestTierCanReach() throws IOException {
        assertThat(ArmoryPage.sheet(CHARACTER).professions()).containsExactly(
                new ArmorySheet.Profession("Mining", "mining", 70, 150, "mining.jpg"),
                new ArmorySheet.Profession("Engineering", "engineering", 75, 150, null),
                new ArmorySheet.Profession("First Aid", "first-aid", 79, 150, null));
    }

    @Test
    void eachItemCarriesItsTooltipInTheOrderOfTheGame() throws IOException {
        List<ArmorySheet.Item> gear = ArmoryPage.sheet(CHARACTER).gear();

        assertThat(gear.getFirst()).isEqualTo(new ArmorySheet.Item("MAIN_HAND", "Main Hand", "Blade of Cunning",
                "UNCOMMON", "blade.jpg", List.of(
                        new ArmorySheet.Line("Item Level 13", null, "#ffd100"),
                        new ArmorySheet.Line("Binds when picked up", null, null),
                        new ArmorySheet.Line("One-Hand", "Dagger", null),
                        new ArmorySheet.Line("9 - 18 Damage", "Speed 2.00", null),
                        new ArmorySheet.Line("(6.8 damage per second)", null, null),
                        new ArmorySheet.Line("+2 Agility", null, null),
                        new ArmorySheet.Line("Durability 30 / 30", null, null))));
        assertThat(gear.get(1).lines()).containsExactly(new ArmorySheet.Line("62 Armor", null, null));
    }

    @Test
    void statisticsAreWrittenAsTheArmoryShowsThem() throws IOException {
        assertThat(ArmoryPage.sheet(CHARACTER).stats()).containsExactly(new ArmorySheet.StatGroup("Attack", List.of(
                new ArmorySheet.Stat("Damage", "20 - 29"),
                new ArmorySheet.Stat("Critical Strike", "5.26%"),
                new ArmorySheet.Stat("Attack Speed", "1.7"),
                new ArmorySheet.Stat("Newstat", "80"))));
    }

    @Test
    void talentsComeWithTheirTree() throws IOException {
        assertThat(ArmoryPage.sheet(CHARACTER).specs()).containsExactly(new ArmorySheet.Spec("Assassination", "a.jpg", 2,
                List.of(new ArmorySheet.Talent("Relentless Strikes", 2, "r.jpg", "Passive", "Hits."))));
    }

    @Test
    void aPageThatChangedItsFormatIsAnErrorInsteadOfAnEmptySheet() {
        assertThatThrownBy(() -> ArmoryPage.match("<html></html>", "Uno")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> ArmoryPage.match(page("{\"other\":1}"), "Uno")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> ArmoryPage.sheet(page("{\"layout\":true}"))).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> ArmoryPage.sheet(page("{not json"))).isInstanceOf(IOException.class);
    }

    @Test
    void iconsAreAskedFromTheGeneralFolderOfTheIconHost() {
        assertThat(ArmoryPage.iconHost("https://render.worldofwarcraft.com/forever-eu/icons/56/135321.jpg"))
                .isEqualTo("https://render.worldofwarcraft.com/eu/icons/56/135321.jpg");
        assertThat(ArmoryPage.iconHost("https://render.worldofwarcraft.com/forever-eu/character/1/4-main-raw.png"))
                .isEqualTo("https://render.worldofwarcraft.com/forever-eu/character/1/4-main-raw.png");
    }

    @Test
    void wholeValuesAreRounded() throws IOException {
        assertThat(ArmoryPage.valueOf(new ObjectMapper().readTree("{\"type\":\"WHOLE\",\"value\":279.6}"))).isEqualTo("280");
    }

    private static String page(String model) {
        return "<html><head><script src=\"a.js\"></script><script id=\"model\">model = " + model + ";</script></head></html>";
    }
}
