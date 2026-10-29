package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two ways a tooltip header is written: one line of parts and the tooltip's own lines.
 *
 * @author adrianpmdev
 */
class SpellTooltipTest {

    @Test
    void partsFollowTheTooltipOrder() {
        SpellTooltip tooltip = new SpellTooltip(false, List.of("25 Energy"), "1 to 5 Combo Points",
                List.of("Melee Range"), "Instant", "10 sec cooldown", List.of("Requires Daggers"),
                List.of("Reagents: Flash Powder"));

        assertThat(tooltip.parts()).containsExactly("25 Energy", "1 to 5 Combo Points", "Melee Range", "Instant",
                "10 sec cooldown", "Requires Daggers", "Reagents: Flash Powder");
        assertThat(tooltip.lines()).containsExactly(List.of("25 Energy", "Melee Range"),
                List.of("1 to 5 Combo Points", ""), List.of("Instant", "10 sec cooldown"),
                List.of("Requires Daggers", ""), List.of("Reagents: Flash Powder", ""));
    }

    @Test
    void enemyRangeGoesOnItsOwnLineAbove() {
        SpellTooltip tooltip = new SpellTooltip(false, List.of("150 Mana"), null,
                List.of("Enemy: 36 yd range", "Friendly: 40 yd range"), "Channeled", null, List.of(), List.of());

        assertThat(tooltip.lines()).containsExactly(List.of("", "Enemy: 36 yd range"),
                List.of("150 Mana", "Friendly: 40 yd range"), List.of("Channeled", ""));
    }

    @Test
    void passiveSpellsOnlySaySo() {
        assertThat(SpellTooltip.passiveSpell().parts()).isEmpty();
        assertThat(SpellTooltip.passiveSpell().lines()).containsExactly(List.of("Passive", ""));
    }
}
