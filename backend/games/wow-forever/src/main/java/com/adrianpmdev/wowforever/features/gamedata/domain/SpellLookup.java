package com.adrianpmdev.wowforever.features.gamedata.domain;

import java.util.List;
import java.util.Optional;

/**
 * Finds a spell by id: tooltips quote the values of other spells ({@code $12345s1}) and embed their texts.
 *
 * @author adrianpmdev
 */
@FunctionalInterface
public interface SpellLookup {

    /**
     * A spell.
     *
     * @param id spell id
     * @return the spell, or empty when the client has none with that id
     */
    Optional<SpellInfo> spell(int id);

    /**
     * The parts of the line a tooltip shows above the text of a spell: cost, range, cast time, cooldown and
     * requirements, in that order.
     *
     * @param id spell id
     * @return the parts, or an empty list for a passive or unknown spell
     */
    default List<String> tooltipLine(int id) {
        return List.of();
    }

    /**
     * What casting a spell costs, as its tooltip writes it ("55% of base Mana", "30 Rage").
     *
     * @param id spell id
     * @return the cost, or empty when it costs nothing
     */
    default Optional<String> cost(int id) {
        return Optional.empty();
    }
}
