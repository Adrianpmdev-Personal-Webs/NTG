package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Writes the {@code racials} section (every playable race by faction, with its classes and racial abilities) and the
 * {@code class_racials} section (abilities a class only gets with some races of a faction, such as the racial
 * spells of priests).
 * <p>
 * Playable races are those the client lets create a character with; racial abilities are the abilities of the skill
 * lines the client names "... Racial", for that race.
 *
 * @author adrianpmdev
 */
final class RacialsExporter {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final String RACIAL_SKILL_LINE = "racial";
    /** Category of the skill lines of class spells (the spellbook tabs of a class). */
    private static final int CLASS_SKILL_CATEGORY = 7;
    private static final int HORDE = 1;
    private static final int MASK_BITS = 32;

    private final ClientTables tables;
    private final ClientSpells spells;

    /** A playable race: its row and the classes it can be, in class order. */
    private record Race(Row row, List<Row> classes) {

        boolean isIn(Row ability) {
            int bit = row.integer("PlayableRaceBit");
            long mask = bit < MASK_BITS ? ability.longValue("RaceMasks_0") : ability.longValue("RaceMasks_1");

            return bit >= 0 && (mask & (1L << (bit % MASK_BITS))) != 0;
        }

        boolean canBe(int classId) {
            return classes.stream().anyMatch(gameClass -> gameClass.integer("ID") == classId);
        }

        boolean horde() {
            return row.integer("Alliance") == HORDE;
        }
    }

    RacialsExporter(ClientTables tables, ClientSpells spells) {
        this.tables = tables;
        this.spells = spells;
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

        for (Race race : races()) {
            ObjectNode written = (race.horde() ? horde : alliance).addObject();
            written.put("race", race.row().text("Name_lang"));
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
        List<Race> races = races();
        List<Row> classes = new ArrayList<>(tables.get(ClientTable.CHR_CLASSES).rows());
        classes.sort(Comparator.comparingInt(row -> row.integer("ID")));
        ObjectNode section = JSON.objectNode();

        for (Row gameClass : classes) {
            int classId = gameClass.integer("ID");
            Map<String, List<Row>> byRace = new LinkedHashMap<>();

            for (Row ability : tables.get(ClientTable.SKILL_LINE_ABILITY).rows()) {
                Row line = skillLines.get(ability.integer("SkillLine"));

                if (line == null || line.integer("CategoryID") != CLASS_SKILL_CATEGORY
                        || (ability.integer("ClassMask") & (1 << (classId - 1))) == 0) {
                    continue;
                }

                List<Race> with = races.stream().filter(race -> race.canBe(classId) && race.isIn(ability)).toList();

                if (!with.isEmpty() && !wholeFaction(races, with, classId)) {
                    with.forEach(race -> byRace.computeIfAbsent(race.row().text("Name_lang"), name -> new ArrayList<>())
                            .add(ability));
                }
            }

            if (!byRace.isEmpty()) {
                ObjectNode racesNode = section.putObject(gameClass.text("Name_lang")).putObject("races");
                byRace.forEach((race, abilities) -> racesNode.set(race, abilities(abilities, true)));
            }
        }

        return section;
    }

    /** Whether the races given an ability are every race of their faction that can be the class (no race choice). */
    private static boolean wholeFaction(List<Race> races, List<Race> with, int classId) {
        Set<Boolean> factions = new HashSet<>();
        with.forEach(race -> factions.add(race.horde()));

        return factions.stream().allMatch(horde -> races.stream()
                .filter(race -> race.horde() == horde && race.canBe(classId))
                .allMatch(with::contains));
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

    /** Races a character can be created with (those with classes), in client order. */
    private List<Race> races() {
        Map<Integer, Row> classesById = tables.get(ClientTable.CHR_CLASSES).indexBy("ID");
        Map<Integer, List<Row>> classesByRace = new TreeMap<>();

        for (Row combination : tables.get(ClientTable.CHAR_BASE_INFO).rows()) {
            Row gameClass = classesById.get(combination.integer("ClassID"));

            if (gameClass != null) {
                classesByRace.computeIfAbsent(combination.integer("RaceID"), race -> new ArrayList<>()).add(gameClass);
            }
        }

        Map<Integer, Row> racesById = tables.get(ClientTable.CHR_RACES).indexBy("ID");
        List<Race> races = new ArrayList<>();

        classesByRace.forEach((raceId, classes) -> {
            Row race = racesById.get(raceId);

            if (race != null) {
                List<Row> ordered = new ArrayList<>(classes);
                ordered.sort(Comparator.comparingInt(row -> row.integer("ID")));
                races.add(new Race(race, List.copyOf(ordered)));
            }
        });

        return races;
    }
}
