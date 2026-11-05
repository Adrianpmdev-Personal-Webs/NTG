package com.adrianpmdev.wowforever.features.characters.infrastructure.armory;

import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the pages of the official armory, which carry their data as JSON in a script called model. That format is not
 * documented and can change: anything essential missing is an error, never an empty sheet.
 *
 * @author adrianpmdev
 */
final class ArmoryPage {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MODEL = "<script id=\"model\">model = ";
    private static final String CHARACTER_CATEGORY = "CHARACTER";
    private static final String GREEN = "#1eff00";
    private static final String YELLOW = "#ffd100";
    private static final String FOREVER_ICONS = "/forever-eu/icons/";
    private static final String GENERAL_ICONS = "/eu/icons/";

    /** The armory only gives the code of each statistic. */
    private static final Map<String, String> STAT_NAMES = Map.ofEntries(
            Map.entry("HEALTH", "Health"), Map.entry("MANA", "Mana"), Map.entry("ENERGY", "Energy"),
            Map.entry("RAGE", "Rage"), Map.entry("FOCUS", "Focus"), Map.entry("ITEMLEVEL", "Item Level"),
            Map.entry("MOVEMENTSPEED", "Movement Speed"), Map.entry("STRENGTH", "Strength"),
            Map.entry("AGILITY", "Agility"), Map.entry("INTELLECT", "Intellect"), Map.entry("STAMINA", "Stamina"),
            Map.entry("SPIRIT", "Spirit"), Map.entry("CRITICALSTRIKE", "Critical Strike"), Map.entry("HASTE", "Haste"),
            Map.entry("MASTERY", "Mastery"), Map.entry("BONUSARMOR", "Bonus Armor"), Map.entry("LEECH", "Leech"),
            Map.entry("VERSATILITY", "Versatility"), Map.entry("AVOIDANCE", "Avoidance"),
            Map.entry("DAMAGE", "Damage"), Map.entry("ATTACKPOWER", "Attack Power"),
            Map.entry("ATTACKSPEED", "Attack Speed"), Map.entry("ATTACKDPS", "DPS"),
            Map.entry("SPELLPOWER", "Spell Power"), Map.entry("MANAREGEN", "Mana Regen"), Map.entry("ARMOR", "Armor"),
            Map.entry("DODGE", "Dodge"), Map.entry("PARRY", "Parry"), Map.entry("BLOCK", "Block"));

    private ArmoryPage() {
    }

    /**
     * Finds a character in a search page. The search also gives similar names, so only the exact one counts.
     *
     * @param html page of the search
     * @param name full name wanted, in any case
     * @return the character, or empty when the armory does not have it
     * @throws IOException when the page is not what was expected
     */
    static Optional<ArmoryMatch> match(String html, String name) throws IOException {
        JsonNode categories = model(html).path("categories");

        if (!categories.isArray()) {
            throw new IOException("The armory search has no categories");
        }

        for (JsonNode category : categories) {
            if (CHARACTER_CATEGORY.equals(category.path("enum").asText())) {
                for (JsonNode result : category.path("results")) {
                    if (fullName(result).equalsIgnoreCase(name) && result.path("slug").isTextual()) {
                        return Optional.of(new ArmoryMatch(result.path("slug").asText(), lastUpdated(result)));
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Reads a character page.
     *
     * @param html page of the character
     * @return the character
     * @throws IOException when the page is not what was expected
     */
    static ArmorySheet sheet(String html) throws IOException {
        JsonNode character = model(html).path("reactMounts").path(0).path("initialState").path("character");

        if (!character.path("slug").isTextual() || !character.path("level").isInt()
                || !character.path("class").path("name").isTextual()
                || !character.path("race").path("name").isTextual()) {
            throw new IOException("The armory page has no character");
        }

        String className = character.path("class").path("name").asText();
        String raceName = character.path("race").path("name").asText();
        JsonNode guild = character.path("guild").path("name");

        return new ArmorySheet(fullName(character), character.path("level").asInt(), className,
                GameDataService.slugOf(className), raceName, GameDataService.slugOf(raceName),
                character.path("faction").path("enum").asText(null), character.path("gender").path("name").asText(null),
                guild.isTextual() ? guild.asText() : null, character.path("averageItemLevel").asInt(),
                lastUpdated(character), images(character), professions(character.path("professions")),
                gear(character.path("gear")), stats(character.path("stats").path("groups")),
                specs(character.path("specs")), character.path("pvp").path("honorableKills").path("value").asInt());
    }

    private static ArmorySheet.Images images(JsonNode character) {
        JsonNode render = character.path("render");

        return new ArmorySheet.Images(urlOf(character.path("media").path("avatar")),
                urlOf(render.path("foreground")) != null ? urlOf(render.path("foreground"))
                        : urlOf(character.path("media").path("main-raw")),
                urlOf(render.path("background")));
    }

    /** A profession can list several tiers: what it can reach is told by the highest one. */
    private static List<ArmorySheet.Profession> professions(JsonNode professions) {
        List<ArmorySheet.Profession> known = new ArrayList<>();

        for (JsonNode list : List.of(professions.path("primaries"), professions.path("secondaries"))) {
            for (JsonNode profession : list) {
                JsonNode highest = null;

                for (JsonNode tier : profession.path("tiers")) {
                    if (highest == null
                            || tier.path("maxSkillPoints").asInt() > highest.path("maxSkillPoints").asInt()) {
                        highest = tier;
                    }
                }

                if (highest != null) {
                    String name = profession.path("name").asText();

                    known.add(new ArmorySheet.Profession(name, GameDataService.slugOf(name),
                            highest.path("skillPoints").asInt(), highest.path("maxSkillPoints").asInt(),
                            urlOf(profession.path("icon"))));
                }
            }
        }

        return known;
    }

    private static List<ArmorySheet.Item> gear(JsonNode gear) {
        List<ArmorySheet.Item> items = new ArrayList<>();

        for (JsonNode item : gear) {
            String icon = null;

            for (JsonNode asset : item.path("media").path("content").path("assets")) {
                if ("icon".equals(asset.path("key").asText())) {
                    icon = iconHost(asset.path("value").asText());
                }
            }

            items.add(new ArmorySheet.Item(item.path("slot").path("type").asText(),
                    item.path("slot").path("name").asText(), item.path("name").asText(),
                    item.path("quality").path("type").asText("COMMON"), icon, tooltip(item)));
        }

        return items;
    }

    /** The tooltip in the order the game shows it. */
    private static List<ArmorySheet.Line> tooltip(JsonNode item) {
        List<ArmorySheet.Line> lines = new ArrayList<>();

        add(lines, item.path("level").path("display_string"), null, YELLOW);
        add(lines, item.path("binding").path("name"), null, null);
        add(lines, item.path("unique_equipped"), null, null);

        JsonNode slot = item.path("inventory_type").path("name");
        JsonNode kind = item.path("item_subclass").path("name");

        if (slot.isTextual()) {
            lines.add(new ArmorySheet.Line(slot.asText(), kind.isTextual() ? kind.asText() : null, null));
        }

        JsonNode armor = item.path("armor");

        if (armor.path("display").path("display_string").isTextual()) {
            add(lines, armor.path("display").path("display_string"), null, null);
        } else if (armor.path("value").isNumber()) {
            lines.add(new ArmorySheet.Line(armor.path("value").asInt() + " Armor", null, null));
        }

        JsonNode weapon = item.path("weapon");

        if (weapon.path("damage").path("display_string").isTextual()) {
            lines.add(new ArmorySheet.Line(weapon.path("damage").path("display_string").asText(),
                    weapon.path("attack_speed").path("display_string").asText(null), null));
        }

        add(lines, weapon.path("dps").path("display_string"), null, null);

        for (JsonNode stat : item.path("stats")) {
            add(lines, stat.path("display").path("display_string"), null, colorOf(stat.path("display").path("color")));
        }

        add(lines, item.path("requirements").path("level").path("display_string"), null, null);
        add(lines, item.path("requirements").path("playable_classes").path("display_string"), null, null);
        add(lines, item.path("durability").path("display_string"), null, null);

        for (JsonNode spell : item.path("spells")) {
            add(lines, spell.path("description"), null, GREEN);
        }

        add(lines, item.path("description"), null, YELLOW);

        return lines;
    }

    private static void add(List<ArmorySheet.Line> lines, JsonNode text, String right, String color) {
        if (text.isTextual() && !text.asText().isBlank()) {
            lines.add(new ArmorySheet.Line(text.asText(), right, color));
        }
    }

    private static String colorOf(JsonNode color) {
        if (!color.path("r").isNumber()) {
            return null;
        }

        String hex = String.format(Locale.ROOT, "#%02x%02x%02x", color.path("r").asInt(), color.path("g").asInt(),
                color.path("b").asInt());

        return "#ffffff".equals(hex) ? null : hex;
    }

    private static List<ArmorySheet.StatGroup> stats(JsonNode groups) {
        List<ArmorySheet.StatGroup> result = new ArrayList<>();

        for (JsonNode group : groups) {
            List<ArmorySheet.Stat> stats = new ArrayList<>();

            for (JsonNode stat : group.path("stats")) {
                String code = stat.path("enum").asText();

                stats.add(new ArmorySheet.Stat(STAT_NAMES.getOrDefault(code, readable(code)),
                        valueOf(stat.path("value"))));
            }

            result.add(new ArmorySheet.StatGroup(group.path("name").asText(), stats));
        }

        return result;
    }

    /** A value written as the armory writes it: whole numbers, percentages, decimals or a range. */
    static String valueOf(JsonNode value) {
        return switch (value.path("type").asText()) {
            case "RANGE" -> whole(value.path("min").path("value")) + " - " + whole(value.path("max").path("value"));
            case "PERCENTAGE" -> decimal(value.path("value")) + "%";
            case "DECIMAL" -> decimal(value.path("value"));
            default -> whole(value.path("value"));
        };
    }

    private static String whole(JsonNode number) {
        return String.valueOf(Math.round(number.asDouble()));
    }

    private static String decimal(JsonNode number) {
        return BigDecimal.valueOf(number.asDouble()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
                .toPlainString();
    }

    private static String readable(String code) {
        return code.isEmpty() ? code : code.charAt(0) + code.substring(1).toLowerCase(Locale.ROOT);
    }

    private static List<ArmorySheet.Spec> specs(JsonNode specs) {
        List<ArmorySheet.Spec> result = new ArrayList<>();

        for (JsonNode spec : specs) {
            List<ArmorySheet.Talent> talents = new ArrayList<>();

            for (JsonNode talent : spec.path("talents")) {
                talents.add(new ArmorySheet.Talent(talent.path("name").asText(), talent.path("rank").asInt(),
                        urlOf(talent.path("icon")), talent.path("cast").asText(null),
                        talent.path("description").asText("")));
            }

            result.add(new ArmorySheet.Spec(spec.path("name").asText(), urlOf(spec.path("icon")),
                    spec.path("spentPoints").asInt(), talents));
        }

        return result;
    }

    private static String urlOf(JsonNode picture) {
        return picture.path("url").isTextual() ? iconHost(picture.path("url").asText()) : null;
    }

    /**
     * Blizzard's icon host refuses some icons under the WoW Forever folder that it serves under the general one, so
     * every icon is asked there.
     */
    static String iconHost(String url) {
        return url.replace(FOREVER_ICONS, GENERAL_ICONS);
    }

    private static String fullName(JsonNode character) {
        return (character.path("name").asText() + " " + character.path("surname").asText()).trim();
    }

    private static Instant lastUpdated(JsonNode character) {
        JsonNode epoch = character.path("lastUpdatedTimestamp").path("epoch");

        return epoch.isNumber() ? Instant.ofEpochMilli(epoch.asLong()) : null;
    }

    private static JsonNode model(String html) throws IOException {
        int start = html.indexOf(MODEL);

        if (start < 0) {
            throw new IOException("The armory page has no model");
        }

        try {
            return JSON.readTree(html.substring(start + MODEL.length()));
        } catch (JsonProcessingException exception) {
            throw new IOException("The armory model could not be read", exception);
        }
    }
}
