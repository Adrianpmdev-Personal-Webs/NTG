package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The races a character can be created with (those the client gives classes), and which of them an ability of the
 * client is for: abilities carry a mask of races, and some class spells are only given to some races of a faction.
 *
 * @author adrianpmdev
 */
final class PlayableRaces {

    private static final int HORDE = 1;
    private static final int MASK_BITS = 32;

    private final List<Race> races;

    /**
     * A playable race.
     *
     * @param row     its client row
     * @param classes the classes it can be, in class order
     */
    record Race(Row row, List<Row> classes) {

        /** Whether an ability (a skill line ability row) is given to this race. */
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

        String name() {
            return row.text("Name_lang");
        }
    }

    private PlayableRaces(List<Race> races) {
        this.races = races;
    }

    /**
     * Reads the playable races, in client order.
     *
     * @param tables client tables
     * @return the races
     */
    static PlayableRaces read(ClientTables tables) {
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

        return new PlayableRaces(List.copyOf(races));
    }

    /** Every playable race. */
    List<Race> all() {
        return races;
    }

    /**
     * The races of a class an ability is only for, when it is not for every race of their faction ("Desperate
     * Prayer" for Human and Dwarf priests). Empty when the ability is for any race of the class.
     *
     * @param ability skill line ability row
     * @param classId class
     * @return the races, or an empty list
     */
    List<Race> onlyFor(Row ability, int classId) {
        List<Race> with = races.stream().filter(race -> race.canBe(classId) && race.isIn(ability)).toList();

        return with.isEmpty() || wholeFactions(with, classId) ? List.of() : with;
    }

    /** Whether the races are every race of their factions that can be the class (no race choice). */
    private boolean wholeFactions(List<Race> with, int classId) {
        Set<Boolean> factions = new HashSet<>();
        with.forEach(race -> factions.add(race.horde()));

        return factions.stream().allMatch(horde -> races.stream()
                .filter(race -> race.horde() == horde && race.canBe(classId))
                .allMatch(with::contains));
    }
}
