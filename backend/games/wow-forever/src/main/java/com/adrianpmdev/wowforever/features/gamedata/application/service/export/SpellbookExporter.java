package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the {@code spellbooks} section (per class, one tab per talent tree with every spell and rank, the level
 * each rank is learned at, icons and which spells come from talents) and the {@code spell_desc} section (the tooltip
 * of every rank, keyed {@code Class|Spell|Rank}).
 * <p>
 * A tab holds the abilities of the class skill line named like the talent tree ("Shadow" is the skill line "Shadow
 * Magic"); the extra tabs of the settings (a hunter's pets, a warlock's demons, a rogue's poisons) hold the skill
 * lines they name. Left out are the abilities the client does not teach that way (see ClassSkillLines.NOT_TAUGHT),
 * except active talents, and passive talents. The client has no General tab and does not say which trainer teaches
 * each rank, so both are left out.
 *
 * @author adrianpmdev
 */
final class SpellbookExporter {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final String KEY_SEPARATOR = "|";
    private static final Pattern RANK_NUMBER = Pattern.compile("(\\d+)");

    private final ClientTables tables;
    private final ClientSpells spells;
    private final Map<String, Map<String, List<String>>> extraTabs;
    private final GameDataConfig.Talents talentRules;
    private final ClassSkillLines classLines;

    /**
     * The two sections.
     *
     * @param spellbooks        spellbook of each class
     * @param spellDescriptions tooltip of every rank
     */
    record Sections(ObjectNode spellbooks, ObjectNode spellDescriptions) {
    }

    /** One rank of a spell in a tab. */
    private record Entry(String name, String rank, int spellId) {
    }

    /**
     * Builds the exporter.
     *
     * @param tables      client tables
     * @param spells      client spells
     * @param extraTabs   tabs besides the talent trees, by class name (see GameDataConfig.Spellbook)
     * @param talentRules rules of the talent calculator: a talent's spell is learned when its row opens
     */
    SpellbookExporter(ClientTables tables, ClientSpells spells, Map<String, Map<String, List<String>>> extraTabs,
                      GameDataConfig.Talents talentRules) {
        this.tables = tables;
        this.spells = spells;
        this.extraTabs = extraTabs == null ? Map.of() : extraTabs;
        this.talentRules = talentRules;
        this.classLines = new ClassSkillLines(tables);
    }

    /**
     * Writes both sections for the classes of the talents section.
     *
     * @param talents the talents section, which names the classes and their talents
     * @param build   game build the data was read from
     * @return the sections
     */
    Sections export(ObjectNode talents, String build) {
        Map<String, Row> classesByName = new LinkedHashMap<>();
        tables.get(ClientTable.CHR_CLASSES).rows().forEach(row -> classesByName.put(row.text("Name_lang"), row));
        ObjectNode spellbooks = JSON.objectNode();
        ObjectNode descriptions = JSON.objectNode();

        talents.properties().forEach(entry -> {
            Row gameClass = classesByName.get(entry.getKey());

            if (gameClass != null) {
                spellbooks.set(entry.getKey(), spellbook(gameClass, talentLevels(entry.getValue()), build,
                        descriptions));
            }
        });

        return new Sections(spellbooks, descriptions);
    }

    private ObjectNode spellbook(Row gameClass, Map<String, Integer> talentLevels, String build,
                                 ObjectNode descriptions) {
        int classId = gameClass.integer("ID");
        String className = gameClass.text("Name_lang");
        ObjectNode book = JSON.objectNode();
        book.putArray("general");
        ArrayNode tabs = book.putArray("tabs");
        ObjectNode levels = JSON.objectNode();
        ObjectNode icons = JSON.objectNode();
        Set<String> fromTalents = new TreeSet<>();

        Map<String, Set<Integer>> skillLinesByTab = classLines.spellbookLines(classId,
                extraTabs.getOrDefault(className, Map.of()));

        for (Map.Entry<String, Set<Integer>> tab : skillLinesByTab.entrySet()) {
            List<Entry> entries = entries(tab.getValue(), classId, talentLevels.keySet());
            ObjectNode tabNode = tabs.addObject();
            tabNode.put("name", tab.getKey());
            ArrayNode tabSpells = tabNode.putArray("spells");

            for (Entry entry : entries) {
                tabSpells.addArray().add(entry.name()).add(entry.rank());
                ArrayNode rankLevels = levels.has(entry.name()) ? (ArrayNode) levels.get(entry.name())
                        : levels.putArray(entry.name());
                int level = learnLevel(entry, talentLevels);
                rankLevels.add(level);

                if (!icons.has(entry.name())) {
                    icons.put(entry.name(), spells.icon(entry.spellId()));
                }

                if (talentLevels.containsKey(entry.name())) {
                    fromTalents.add(entry.name());
                }

                descriptions.set(className + KEY_SEPARATOR + entry.name() + KEY_SEPARATOR + entry.rank(),
                        rankText(spells.taughtSpell(entry.spellId()), level));
            }
        }

        book.set("levels", levels);
        ArrayNode talentsNode = book.putArray("talents");
        fromTalents.forEach(talentsNode::add);
        book.set("icons", icons);
        book.putArray("gone");
        book.put("build", build);

        return book;
    }

    /**
     * The level a rank is learned at: its own, or that of the spell it teaches, or for a talent's spell the level its
     * row opens at.
     */
    private int learnLevel(Entry entry, Map<String, Integer> talentLevels) {
        int level = spells.learnLevel(entry.spellId());

        if (level <= 1) {
            level = spells.learnLevel(spells.taughtSpell(entry.spellId()));
        }

        return level <= 1 ? talentLevels.getOrDefault(entry.name(), level) : level;
    }

    /** The tooltip of one rank: its lines, text, learn level, school and spell power coefficient. */
    private ObjectNode rankText(int spellId, int level) {
        ObjectNode text = JSON.objectNode();
        ArrayNode lines = text.putArray("l");
        spells.tooltip(spellId).lines().forEach(line -> lines.addArray().add(line.get(0)).add(line.get(1)));
        text.put("d", spells.description(spellId));
        text.put("lv", "Learned at level " + level);
        text.put("id", spellId);
        putIfPresent(text, "sc", spells.school(spellId));
        putIfPresent(text, "co", spells.coefficient(spellId));

        return text;
    }

    /** The ranks some skill lines give the class, one per name and rank, by name and then rank number. */
    private List<Entry> entries(Set<Integer> skillLines, int classId, Set<String> talentNames) {
        Map<String, Entry> byKey = new LinkedHashMap<>();

        for (Row ability : tables.get(ClientTable.SKILL_LINE_ABILITY).rows()) {
            int spellId = ability.integer("Spell");
            String name = spells.name(spellId);

            if (!skillLines.contains(ability.integer("SkillLine")) || !ClassSkillLines.forClass(ability, classId)
                    || name.isEmpty() || spells.hidden(spellId) || !taught(ability, talentNames.contains(name))) {
                continue;
            }

            byKey.putIfAbsent(name + KEY_SEPARATOR + spells.rankLabel(spellId),
                    new Entry(name, spells.rankLabel(spellId), spellId));
        }

        List<Entry> entries = new ArrayList<>(byKey.values());
        entries.sort(Comparator.comparing(Entry::name).thenComparingInt(entry -> rankNumber(entry.rank())));

        return entries;
    }

    /** Whether the spellbook lists an ability: taught by its skill line, or an active talent; never a passive talent. */
    private boolean taught(Row ability, boolean talent) {
        boolean passive = spells.passive(ability.integer("Spell"));

        return talent ? !passive : ability.integer("AcquireMethod") != ClassSkillLines.NOT_TAUGHT;
    }

    private static int rankNumber(String rank) {
        Matcher matcher = RANK_NUMBER.matcher(rank);

        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    /** Every talent of a class in the talents section, with the level its row opens at. */
    private Map<String, Integer> talentLevels(JsonNode classTalents) {
        Map<String, Integer> levels = new HashMap<>();
        classTalents.path("trees").forEach(tree -> tree.path("talents").forEach(talent -> levels.put(
                talent.path("name").asText(),
                talentRules.firstPointLevel() + (talent.path("row").asInt(1) - 1) * talentRules.pointsPerRow())));

        return levels;
    }

    private static void putIfPresent(ObjectNode node, String field, String value) {
        if (value != null) {
            node.put(field, value);
        }
    }
}
