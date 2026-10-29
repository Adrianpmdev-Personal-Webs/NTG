package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import java.util.ArrayList;
import java.util.List;

/**
 * What a spell tooltip shows above the text, and the two ways the data file writes it: as one list of parts (talent
 * cost lines, ability lists) or as the tooltip's own lines of a left and a right side (spellbook).
 *
 * @param passive      the spell is passive: its tooltip only says so
 * @param costs        what casting it costs ("30 Rage", "4% of base mana")
 * @param comboPoints  the combo points it uses ("1 to 5 Combo Points"), or null
 * @param ranges       its range, one part, or two when enemies and allies differ ("Enemy: 36 yd range")
 * @param castTime     "Instant", "Channeled", "Next melee" or "2.5 sec cast"
 * @param cooldown     "6 sec cooldown", or null
 * @param requirements "Requires Battle Stance", "Requires Shields"
 * @param reagents     "Reagents: Soul Shard", "Tools: Water Totem"
 *
 * @author adrianpmdev
 */
record SpellTooltip(boolean passive, List<String> costs, String comboPoints, List<String> ranges, String castTime,
                    String cooldown, List<String> requirements, List<String> reagents) {

    private static final String PASSIVE = "Passive";

    /** The tooltip of a spell the client does not know, or of a passive one. */
    static SpellTooltip passiveSpell() {
        return new SpellTooltip(true, List.of(), null, List.of(), null, null, List.of(), List.of());
    }

    /** Every part in tooltip order; empty for a passive spell. */
    List<String> parts() {
        if (passive) {
            return List.of();
        }

        List<String> parts = new ArrayList<>(costs);
        add(parts, comboPoints);
        parts.addAll(ranges);
        add(parts, castTime);
        add(parts, cooldown);
        parts.addAll(requirements);
        parts.addAll(reagents);

        return List.copyOf(parts);
    }

    /**
     * The tooltip's lines as {@code [left, right]}: cost and range, combo points, cast time and cooldown, then each
     * requirement and reagent. When enemies and allies have different ranges, the enemy range goes on a line of its
     * own above.
     */
    List<List<String>> lines() {
        if (passive) {
            return List.of(List.of(PASSIVE, ""));
        }

        List<List<String>> lines = new ArrayList<>();
        String cost = String.join(", ", costs);

        if (ranges.size() > 1) {
            lines.add(List.of("", ranges.getFirst()));
            lines.add(List.of(cost, ranges.get(1)));
        } else if (!cost.isEmpty() || !ranges.isEmpty()) {
            lines.add(List.of(cost, ranges.isEmpty() ? "" : ranges.getFirst()));
        }

        if (comboPoints != null) {
            lines.add(List.of(comboPoints, ""));
        }

        lines.add(List.of(castTime == null ? "" : castTime, cooldown == null ? "" : cooldown));
        requirements.forEach(requirement -> lines.add(List.of(requirement, "")));
        reagents.forEach(reagent -> lines.add(List.of(reagent, "")));

        return List.copyOf(lines);
    }

    private static void add(List<String> parts, String part) {
        if (part != null) {
            parts.add(part);
        }
    }
}
