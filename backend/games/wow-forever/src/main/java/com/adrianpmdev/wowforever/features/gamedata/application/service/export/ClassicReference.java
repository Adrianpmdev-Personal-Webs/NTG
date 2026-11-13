package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the comparison with Classic Era needs from its client, read once and kept: Classic Era does not change, so its
 * tables are never read again for a new build of WoW Forever. For each class of the Classic client:
 * <ul>
 *     <li>{@code talents}: by name, its tree, row and column (from 1), ranks and the text of rank 1;</li>
 *     <li>{@code spells}: by {@code Spell|Rank}, the text of every rank of the spells its spellbook teaches.</li>
 * </ul>
 *
 * @author adrianpmdev
 */
final class ClassicReference {

    static final String CLASSES = "classes";
    static final String TALENTS = "talents";
    static final String SPELLS = "spells";
    static final String KEY_SEPARATOR = "|";

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final int RANK_SLOTS = 9;

    private final ClientTables tables;
    private final ClientSpells spells;
    private final ClassSkillLines classLines;
    private final Map<String, Map<String, List<String>>> extraTabs;

    /**
     * Builds the reader.
     *
     * @param classicTables tables of the Classic Era client
     * @param playerLevel   level the texts are read for
     * @param extraTabs     spellbook tabs besides the talent trees, by class name (the same skill lines in Classic)
     */
    ClassicReference(ClientTables classicTables, int playerLevel, Map<String, Map<String, List<String>>> extraTabs) {
        this.tables = classicTables;
        this.spells = new ClientSpells(classicTables, playerLevel);
        this.classLines = new ClassSkillLines(classicTables);
        this.extraTabs = extraTabs == null ? Map.of() : extraTabs;
    }

    /**
     * Reads the classes of Classic.
     *
     * @return the {@value #CLASSES} section of the reference
     */
    ObjectNode read() {
        Map<String, Integer> classIds = new LinkedHashMap<>();
        tables.get(ClientTable.CHR_CLASSES).rows()
                .forEach(row -> classIds.put(row.text("Name_lang"), row.integer("ID")));
        ObjectNode classes = JSON.objectNode();

        classIds.forEach((name, id) -> {
            ObjectNode gameClass = classes.putObject(name);
            gameClass.set(TALENTS, talents(id));
            gameClass.set(SPELLS, spellTexts(name, id));
        });

        return classes;
    }

    /** The Classic talents of a class by name. */
    private ObjectNode talents(int classId) {
        Map<Integer, String> tabNames = new HashMap<>();
        classLines.talentTabs(classId).forEach(tab -> tabNames.put(tab.integer("ID"), tab.text("Name_lang")));
        ObjectNode talents = JSON.objectNode();

        for (Row talent : tables.get(ClientTable.CLASSIC_TALENT).rows()) {
            String tree = tabNames.get(talent.integer("TabID"));
            int firstRank = talent.integer("SpellRank_0");

            if (tree != null && firstRank > 0 && !talents.has(spells.name(firstRank))) {
                int ranks = 0;

                while (ranks < RANK_SLOTS && talent.integer("SpellRank_" + ranks) > 0) {
                    ranks++;
                }

                talents.putObject(spells.name(firstRank))
                        .put("tree", tree)
                        .put("row", talent.integer("TierID") + 1)
                        .put("col", talent.integer("ColumnIndex") + 1)
                        .put("max", ranks)
                        .put("text", spells.description(firstRank));
            }
        }

        return talents;
    }

    /** The text of every rank of the Classic spells of a class that Classic teaches, by {@code Spell|Rank}. */
    private ObjectNode spellTexts(String className, int classId) {
        Set<Integer> lines = new HashSet<>();
        classLines.spellbookLines(classId, extraTabs.getOrDefault(className, Map.of())).values()
                .forEach(lines::addAll);
        ObjectNode texts = JSON.objectNode();

        for (Row ability : tables.get(ClientTable.SKILL_LINE_ABILITY).rows()) {
            int spellId = spells.taughtSpell(ability.integer("Spell"));
            String name = spells.name(ability.integer("Spell"));

            if (lines.contains(ability.integer("SkillLine")) && ClassSkillLines.forClass(ability, classId)
                    && ability.integer("AcquireMethod") != ClassSkillLines.NOT_TAUGHT && !name.isEmpty()
                    && !spells.hidden(ability.integer("Spell"))) {
                String key = name + KEY_SEPARATOR + spells.rankLabel(ability.integer("Spell"));

                if (!texts.has(key)) {
                    texts.put(key, spells.description(spellId));
                }
            }
        }

        return texts;
    }
}
