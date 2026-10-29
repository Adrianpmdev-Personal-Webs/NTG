package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The talent tabs of a class and the class skill line behind each one, where the client keeps the spells of that tab
 * of the spellbook. Shared by the spellbook and by the comparison with Classic.
 *
 * @author adrianpmdev
 */
final class ClassSkillLines {

    /** Category of the skill lines of class spells. */
    static final int CLASS_SKILL_CATEGORY = 7;
    /**
     * Abilities the client does not teach through their skill line: copies of a spell listed in another line,
     * effects, and spells not in the game (Classic Era also keeps the runes of its seasons this way).
     */
    static final int NOT_TAUGHT = 3;

    private final ClientTables tables;

    ClassSkillLines(ClientTables tables) {
        this.tables = tables;
    }

    /** The talent tabs of a class, in the game's order. */
    List<Row> talentTabs(int classId) {
        return tables.get(ClientTable.TALENT_TAB).rows().stream()
                .filter(tab -> tab.integer("ClassMask") == classBit(classId))
                .sorted(Comparator.comparingInt(tab -> tab.integer("OrderIndex")))
                .toList();
    }

    /**
     * The class skill line of a talent tab: named like it, or starting with its name ("Shadow" is "Shadow Magic"),
     * and with the most abilities of the class when there are several (two classes have a "Holy" line).
     *
     * @param tabName talent tab name
     * @param classId class
     * @return the skill line id, or 0 when there is none
     */
    int skillLineOf(String tabName, int classId) {
        Set<Integer> candidates = new HashSet<>();
        String name = tabName.toLowerCase(Locale.ROOT);

        for (Row line : tables.get(ClientTable.SKILL_LINE).rows()) {
            String lineName = line.text("DisplayName_lang").toLowerCase(Locale.ROOT);

            if (line.integer("CategoryID") == CLASS_SKILL_CATEGORY
                    && (lineName.equals(name) || lineName.startsWith(name + " "))) {
                candidates.add(line.integer("ID"));
            }
        }

        Map<Integer, Integer> abilitiesByLine = new LinkedHashMap<>();

        for (Row ability : tables.get(ClientTable.SKILL_LINE_ABILITY).rows()) {
            if (candidates.contains(ability.integer("SkillLine")) && forClass(ability, classId)) {
                abilitiesByLine.merge(ability.integer("SkillLine"), 1, Integer::sum);
            }
        }

        return abilitiesByLine.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                .orElse(0);
    }

    /**
     * The skill lines of a class's spellbook: one per talent tab, then those of its extra tabs.
     *
     * @param classId   class
     * @param extraTabs skill line names of the extra tabs, by tab name
     * @return the skill line ids by tab name, in tab order
     */
    Map<String, Set<Integer>> spellbookLines(int classId, Map<String, List<String>> extraTabs) {
        Map<String, Set<Integer>> linesByTab = new LinkedHashMap<>();
        talentTabs(classId).forEach(tab -> linesByTab.put(tab.text("Name_lang"),
                Set.of(skillLineOf(tab.text("Name_lang"), classId))));
        extraTabs.forEach((tab, names) -> linesByTab.put(tab, skillLinesNamed(names)));

        return linesByTab;
    }

    /** The skill lines with one of the names (any category). */
    private Set<Integer> skillLinesNamed(List<String> names) {
        Set<Integer> lines = new HashSet<>();

        for (Row line : tables.get(ClientTable.SKILL_LINE).rows()) {
            if (names.contains(line.text("DisplayName_lang"))) {
                lines.add(line.integer("ID"));
            }
        }

        return lines;
    }

    /** Whether a skill line ability is for a class (for every class when it names none). */
    static boolean forClass(Row ability, int classId) {
        int mask = ability.integer("ClassMask");

        return mask == 0 || (mask & classBit(classId)) != 0;
    }

    /** The bit of a class in the class masks of the client. */
    static int classBit(int classId) {
        return 1 << (classId - 1);
    }
}
