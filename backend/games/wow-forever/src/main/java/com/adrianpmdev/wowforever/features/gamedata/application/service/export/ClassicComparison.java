package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the data of WoW Forever with Classic Era, read from the Classic Era client the same way:
 * <ul>
 *     <li>each talent gets {@code classic}: {@code new} when Classic has no talent with its name for the class,
 *     {@code same} when it has the same ranks and text at the same place, {@code moved} when only the place differs
 *     and {@code changed} otherwise, with Classic's tree, row, column, ranks and text of rank 1;</li>
 *     <li>each spell rank gets {@code cs} ({@code same}, {@code changed} or {@code new}) and, when it changed,
 *     Classic's text in {@code cd};</li>
 *     <li>each spellbook gets {@code gone}: the spells of the class in Classic that WoW Forever no longer has.</li>
 * </ul>
 *
 * @author adrianpmdev
 */
final class ClassicComparison {

    private static final String KEY_SEPARATOR = "|";
    private static final int RANK_SLOTS = 9;

    private final ClientTables tables;
    private final ClientSpells spells;
    private final ClassSkillLines classLines;
    private final Map<String, Map<String, List<String>>> extraTabs;

    /** A talent of Classic. */
    private record ClassicTalent(String tree, int row, int column, int ranks, String text) {
    }

    /**
     * Builds the comparison.
     *
     * @param classicTables tables of the Classic Era client
     * @param playerLevel   level the texts are read for
     * @param extraTabs     spellbook tabs besides the talent trees, by class name (the same skill lines in Classic)
     */
    ClassicComparison(ClientTables classicTables, int playerLevel, Map<String, Map<String, List<String>>> extraTabs) {
        this.tables = classicTables;
        this.spells = new ClientSpells(classicTables, playerLevel);
        this.classLines = new ClassSkillLines(classicTables);
        this.extraTabs = extraTabs == null ? Map.of() : extraTabs;
    }

    /**
     * Adds the comparison to the sections of a data file, in place.
     *
     * @param talents           the talents section
     * @param spellbooks        the spellbooks section
     * @param spellDescriptions the spell_desc section
     */
    void annotate(ObjectNode talents, ObjectNode spellbooks, ObjectNode spellDescriptions) {
        Map<String, Row> classesByName = new HashMap<>();
        tables.get(ClientTable.CHR_CLASSES).rows().forEach(row -> classesByName.put(row.text("Name_lang"), row));

        talents.properties().forEach(entry -> {
            Row gameClass = classesByName.get(entry.getKey());
            Map<String, ClassicTalent> classic = gameClass == null ? Map.of() : talents(gameClass.integer("ID"));
            entry.getValue().path("trees").forEach(tree -> tree.path("talents").forEach(talent ->
                    ((ObjectNode) talent).set("classic", compare(tree.path("name").asText(), talent, classic))));
        });

        spellbooks.properties().forEach(entry -> {
            Row gameClass = classesByName.get(entry.getKey());

            if (gameClass != null) {
                compareSpells(entry.getKey(), gameClass.integer("ID"), (ObjectNode) entry.getValue(),
                        talents.path(entry.getKey()), spellDescriptions);
            }
        });
    }

    /* ─── Talents ─── */

    private ObjectNode compare(String treeName, JsonNode talent, Map<String, ClassicTalent> classic) {
        ObjectNode comparison = ((ObjectNode) talent).objectNode();
        ClassicTalent before = classic.get(talent.path("name").asText());

        if (before == null) {
            return comparison.put("status", "new");
        }

        boolean sameText = sameText(talent.path("desc").path(0).asText(), before.text())
                && talent.path("max").asInt() == before.ranks();
        boolean samePlace = treeName.equals(before.tree()) && talent.path("row").asInt() == before.row()
                && talent.path("col").asInt() == before.column();

        return comparison.put("status", sameText ? (samePlace ? "same" : "moved") : "changed")
                .put("tree", before.tree())
                .put("row", before.row())
                .put("col", before.column())
                .put("max", before.ranks())
                .put("text", before.text());
    }

    /** The Classic talents of a class by name. */
    private Map<String, ClassicTalent> talents(int classId) {
        Map<Integer, String> tabNames = new HashMap<>();
        classLines.talentTabs(classId).forEach(tab -> tabNames.put(tab.integer("ID"), tab.text("Name_lang")));
        Map<String, ClassicTalent> talents = new HashMap<>();

        for (Row talent : tables.get(ClientTable.CLASSIC_TALENT).rows()) {
            String tree = tabNames.get(talent.integer("TabID"));
            int firstRank = talent.integer("SpellRank_0");

            if (tree != null && firstRank > 0) {
                int ranks = 0;

                while (ranks < RANK_SLOTS && talent.integer("SpellRank_" + ranks) > 0) {
                    ranks++;
                }

                talents.putIfAbsent(spells.name(firstRank), new ClassicTalent(tree, talent.integer("TierID") + 1,
                        talent.integer("ColumnIndex") + 1, ranks, spells.description(firstRank)));
            }
        }

        return talents;
    }

    /* ─── Spells ─── */

    private void compareSpells(String className, int classId, ObjectNode book, JsonNode classTalents,
                               ObjectNode spellDescriptions) {
        Map<String, String> classicTexts = spellTexts(className, classId);
        Set<String> foreverNames = new HashSet<>();
        classTalents.path("trees").forEach(tree -> tree.path("talents")
                .forEach(talent -> foreverNames.add(talent.path("name").asText())));

        for (JsonNode tab : book.path("tabs")) {
            for (JsonNode spell : tab.path("spells")) {
                String name = spell.path(0).asText();
                String rankKey = name + KEY_SEPARATOR + spell.path(1).asText();
                JsonNode text = spellDescriptions.path(className + KEY_SEPARATOR + rankKey);
                String before = classicTexts.get(rankKey);
                foreverNames.add(name);

                if (text instanceof ObjectNode rank) {
                    if (before == null) {
                        rank.put("cs", "new");
                    } else if (sameText(rank.path("d").asText(), before)) {
                        rank.put("cs", "same");
                    } else {
                        rank.put("cs", "changed").put("cd", before);
                    }
                }
            }
        }

        Set<String> gone = new TreeSet<>();
        classicTexts.keySet().forEach(key -> gone.add(key.substring(0, key.indexOf(KEY_SEPARATOR))));
        gone.removeAll(foreverNames);
        ArrayNode goneNode = book.putArray("gone");
        gone.forEach(goneNode::add);
    }

    /** The text of every rank of the Classic spells of a class that Classic teaches, by {@code Spell|Rank}. */
    private Map<String, String> spellTexts(String className, int classId) {
        Set<Integer> lines = new HashSet<>();
        classLines.spellbookLines(classId, extraTabs.getOrDefault(className, Map.of())).values()
                .forEach(lines::addAll);
        Map<String, String> texts = new LinkedHashMap<>();

        for (Row ability : tables.get(ClientTable.SKILL_LINE_ABILITY).rows()) {
            int spellId = spells.taughtSpell(ability.integer("Spell"));
            String name = spells.name(ability.integer("Spell"));

            if (lines.contains(ability.integer("SkillLine")) && ClassSkillLines.forClass(ability, classId)
                    && ability.integer("AcquireMethod") != ClassSkillLines.NOT_TAUGHT && !name.isEmpty()
                    && !spells.hidden(ability.integer("Spell"))) {
                texts.putIfAbsent(name + KEY_SEPARATOR + spells.rankLabel(ability.integer("Spell")),
                        spells.description(spellId));
            }
        }

        return texts;
    }

    private static boolean sameText(String first, String second) {
        return normalized(first).equals(normalized(second));
    }

    private static String normalized(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
