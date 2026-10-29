package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes the {@code racials} section (every playable race by faction, with its classes and racial abilities) and the
 * {@code class_racials} section (abilities a class only gets with some races of a faction, such as the racial
 * spells of priests). Racial abilities are the abilities of the skill lines the client names "... Racial", for that
 * race.
 *
 * @author adrianpmdev
 */
final class RacialsExporter {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final String RACIAL_SKILL_LINE = "racial";

    private final ClientTables tables;
    private final ClientSpells spells;
    private final PlayableRaces races;

    RacialsExporter(ClientTables tables, ClientSpells spells, PlayableRaces races) {
        this.tables = tables;
        this.spells = spells;
        this.races = races;
    }

    /**
     * The {@code racials} section.
     *
     * @return races by faction ({@code Alliance}, {@code Horde})
     */
    ObjectNode racials() {
        Map<Integer, Row> skillLines = tables.get(ClientTable.SKILL_LINE).indexBy("ID");
        List<Row> racialAbilities = tables.get(ClientTable.SKILL_LINE_ABILITY).rows().stream()
                .filter(ability -> {
                    Row line = skillLines.get(ability.integer("SkillLine"));

                    return line != null && line.text("DisplayName_lang").toLowerCase(Locale.ROOT)
                            .contains(RACIAL_SKILL_LINE);
                })
                .toList();

        ObjectNode section = JSON.objectNode();
        ArrayNode alliance = section.putArray("Alliance");
        ArrayNode horde = section.putArray("Horde");

        for (PlayableRaces.Race race : races.all()) {
            ObjectNode written = (race.horde() ? horde : alliance).addObject();
            written.put("race", race.name());
            written.put("icon", "race_" + race.row().text("ClientFileString").toLowerCase(Locale.ROOT) + "_male");
            ArrayNode classes = written.putArray("classes");
            race.classes().forEach(gameClass -> classes.add(gameClass.text("Name_lang")));
            written.set("abilities", abilities(racialAbilities.stream().filter(race::isIn).toList(), false));
        }

        return section;
    }

    /**
     * The {@code class_racials} section.
     *
     * @return by class name, the abilities each race gives it
     */
    ObjectNode classRacials() {
        Map<Integer, Row> skillLines = tables.get(ClientTable.SKILL_LINE).indexBy("ID");
        List<Row> classes = new ArrayList<>(tables.get(ClientTable.CHR_CLASSES).rows());
        classes.sort(Comparator.comparingInt(row -> row.integer("ID")));
        ObjectNode section = JSON.objectNode();

        for (Row gameClass : classes) {
            int classId = gameClass.integer("ID");
            Map<String, List<Row>> byRace = new LinkedHashMap<>();

            for (Row ability : tables.get(ClientTable.SKILL_LINE_ABILITY).rows()) {
                Row line = skillLines.get(ability.integer("SkillLine"));

                if (line != null && line.integer("CategoryID") == ClassSkillLines.CLASS_SKILL_CATEGORY
                        && (ability.integer("ClassMask") & ClassSkillLines.classBit(classId)) != 0) {
                    races.onlyFor(ability, classId).forEach(race ->
                            byRace.computeIfAbsent(race.name(), name -> new ArrayList<>()).add(ability));
                }
            }

            if (!byRace.isEmpty()) {
                ObjectNode racesNode = section.putObject(gameClass.text("Name_lang")).putObject("races");
                byRace.forEach((race, abilities) -> racesNode.set(race, abilities(abilities, true)));
            }
        }

        return section;
    }

    /**
     * Abilities as {@code [name, text, icon]}, one per name, hidden spells left out. Of the rows with one name, a
     * class spell keeps its last rank; a racial keeps the version for every class (some racials have one per class)
     * or else the first one.
     */
    private ArrayNode abilities(List<Row> rows, boolean lastRank) {
        Map<String, Row> chosen = new LinkedHashMap<>();

        for (Row row : rows) {
            int spellId = row.integer("Spell");
            String name = spells.name(spellId);
            Row kept = chosen.get(name);

            if (name.isEmpty() || spells.hidden(spellId)) {
                continue;
            }

            if (kept == null || lastRank || kept.integer("ClassMask") != 0 && row.integer("ClassMask") == 0) {
                chosen.put(name, row);
            }
        }

        ArrayNode written = JSON.arrayNode();
        chosen.forEach((name, row) -> written.addArray()
                .add(name)
                .add(spells.abilityText(row.integer("Spell")))
                .add(spells.icon(row.integer("Spell"))));

        return written;
    }
}
