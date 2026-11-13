package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the data of WoW Forever with Classic Era, as the Classic reference kept from its client holds it (see
 * ClassicReference):
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

    private static final String SEPARATOR = ClassicReference.KEY_SEPARATOR;

    private final JsonNode classes;

    /**
     * Builds the comparison.
     *
     * @param classes the classes section of the Classic reference
     */
    ClassicComparison(JsonNode classes) {
        this.classes = classes;
    }

    /**
     * Adds the comparison to the sections of a data file, in place.
     *
     * @param talents           the talents section
     * @param spellbooks        the spellbooks section
     * @param spellDescriptions the spell_desc section
     */
    void annotate(ObjectNode talents, ObjectNode spellbooks, ObjectNode spellDescriptions) {
        talents.properties().forEach(entry -> {
            JsonNode classic = classes.path(entry.getKey()).path(ClassicReference.TALENTS);
            entry.getValue().path("trees").forEach(tree -> tree.path("talents").forEach(talent ->
                    ((ObjectNode) talent).set("classic", compare(tree.path("name").asText(), talent, classic))));
        });

        spellbooks.properties().forEach(entry -> {
            JsonNode gameClass = classes.path(entry.getKey());

            if (!gameClass.isMissingNode()) {
                compareSpells(entry.getKey(), gameClass.path(ClassicReference.SPELLS), (ObjectNode) entry.getValue(),
                        talents.path(entry.getKey()), spellDescriptions);
            }
        });
    }

    /* ─── Talents ─── */

    private ObjectNode compare(String treeName, JsonNode talent, JsonNode classic) {
        ObjectNode comparison = ((ObjectNode) talent).objectNode();
        JsonNode before = classic.path(talent.path("name").asText());

        if (before.isMissingNode()) {
            return comparison.put("status", "new");
        }

        String tree = before.path("tree").asText();
        int row = before.path("row").asInt();
        int column = before.path("col").asInt();
        int ranks = before.path("max").asInt();
        String text = before.path("text").asText();
        boolean sameText = sameText(talent.path("desc").path(0).asText(), text) && talent.path("max").asInt() == ranks;
        boolean samePlace = treeName.equals(tree) && talent.path("row").asInt() == row
                && talent.path("col").asInt() == column;

        return comparison.put("status", sameText ? (samePlace ? "same" : "moved") : "changed")
                .put("tree", tree)
                .put("row", row)
                .put("col", column)
                .put("max", ranks)
                .put("text", text);
    }

    /* ─── Spells ─── */

    private void compareSpells(String className, JsonNode classicTexts, ObjectNode book, JsonNode classTalents,
                               ObjectNode spellDescriptions) {
        Set<String> foreverNames = new HashSet<>();
        classTalents.path("trees").forEach(tree -> tree.path("talents")
                .forEach(talent -> foreverNames.add(talent.path("name").asText())));

        for (JsonNode tab : book.path("tabs")) {
            for (JsonNode spell : tab.path("spells")) {
                String name = spell.path(0).asText();
                String rankKey = name + SEPARATOR + spell.path(1).asText();
                JsonNode text = spellDescriptions.path(className + SEPARATOR + rankKey);
                JsonNode before = classicTexts.path(rankKey);
                foreverNames.add(name);

                if (text instanceof ObjectNode rank) {
                    if (before.isMissingNode()) {
                        rank.put("cs", "new");
                    } else if (sameText(rank.path("d").asText(), before.asText())) {
                        rank.put("cs", "same");
                    } else {
                        rank.put("cs", "changed").put("cd", before.asText());
                    }
                }
            }
        }

        Set<String> gone = new TreeSet<>();
        classicTexts.properties().forEach(entry -> gone.add(entry.getKey().substring(0,
                entry.getKey().indexOf(SEPARATOR))));
        gone.removeAll(foreverNames);
        ArrayNode goneNode = book.putArray("gone");
        gone.forEach(goneNode::add);
    }

    private static boolean sameText(String first, String second) {
        return normalized(first).equals(normalized(second));
    }

    private static String normalized(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
