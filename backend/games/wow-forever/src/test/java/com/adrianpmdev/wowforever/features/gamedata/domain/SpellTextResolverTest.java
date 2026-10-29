package com.adrianpmdev.wowforever.features.gamedata.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The tokens of spell texts, on small made-up spells.
 *
 * @author adrianpmdev
 */
class SpellTextResolverTest {

    private static final int LEVEL = 60;

    private static SpellInfo spell(int id, String description, int durationMs, SpellInfo.Effect... effects) {
        return new SpellInfo(id, "Spell " + id, description, "", "", durationMs, 0, 0, 30, 3, 2, 25, 6000, 5,
                List.of(effects));
    }

    private static SpellInfo.Effect effect(int index, double basePoints) {
        return new SpellInfo.Effect(index, basePoints, 0, 0, 0, 1, 0, 0);
    }

    private static SpellTextResolver resolver(SpellInfo... spells) {
        Map<Integer, SpellInfo> byId = new java.util.HashMap<>();

        for (SpellInfo spell : spells) {
            byId.put(spell.id(), spell);
        }

        return new SpellTextResolver(id -> Optional.ofNullable(byId.get(id)), LEVEL);
    }

    @Test
    void readsEffectValuesAsTheyShowWithoutSign() {
        SpellInfo spell = spell(1, "Reduces the cost by $/10;s1 Rage and slows by $s2% for $d.", 6000,
                effect(0, -30), effect(1, -50));

        assertThat(resolver(spell).description(spell))
                .isEqualTo("Reduces the cost by 3 Rage and slows by 50% for 6 sec.");
    }

    @Test
    void rankValuesReplaceTheBaseValue() {
        SpellInfo spell = spell(1, "Increases your chance by $s1%.", 0, effect(0, 1));

        assertThat(resolver(spell).description(spell, Map.of(0, 3.0))).isEqualTo("Increases your chance by 3%.");
    }

    @Test
    void valuesWithASpreadShowTheirRange() {
        SpellInfo spell = spell(1, "Causes $s1 damage.", 0, new SpellInfo.Effect(0, 430, 0, 0.0434782, 0, 1, 0, 0));

        assertThat(resolver(spell).description(spell)).isEqualTo("Causes 421 to 439 damage.");
    }

    @Test
    void valuesGrowWithLevelUpToTheSpellLimit() {
        SpellInfo spell = new SpellInfo(1, "Nova", "Heals for $s1.", "", "", 0, 20, 26, 0, 0, 0, 0, 0, 0,
                List.of(new SpellInfo.Effect(0, 27, 0.2, 0, 0, 1, 0, 0)));

        assertThat(resolver(spell).description(spell)).isEqualTo("Heals for 28.");
    }

    @Test
    void formulasAreRoundedUnlessTheyAskForDecimals() {
        SpellInfo spell = spell(1, "Deals ${$m1*$m2/100} and ${$m1/10}.1 more.", 0, effect(0, 23), effect(1, 75));

        assertThat(resolver(spell).description(spell)).isEqualTo("Deals 17 and 2.3 more.");
    }

    @Test
    void readsOtherSpellsAndTheirTexts() {
        SpellInfo other = spell(2, "Stuns for $d.", 4000, effect(0, 1));
        SpellInfo spell = spell(1, "Strikes for $2s1 and stuns for $2d. $@spelldesc2", 0, effect(0, 5));

        assertThat(resolver(spell, other).description(spell))
                .isEqualTo("Strikes for 1 and stuns for 4 sec. Stuns for 4 sec.");
    }

    @Test
    void choicesTakeTheBranchOfACharacterWithoutTheSpell() {
        SpellInfo spell = spell(1, "Heals$?s12345[ and shields][]. $?!s99[Always.][Never.]", 0, effect(0, 1));

        assertThat(resolver(spell).description(spell)).isEqualTo("Heals. Always.");
    }

    @Test
    void unmetRequirementsInRedAreLeftOut() {
        SpellInfo spell = spell(1, "$?a5487[|cFFFFFFFFRequires Bear Form|R][|cFFFF2020Requires Bear Form|R]\nCharge.",
                0, effect(0, 1));

        assertThat(resolver(spell).description(spell)).isEqualTo("Charge.");
    }

    @Test
    void namedValuesAreDefinedByTheSpell() {
        SpellInfo spell = new SpellInfo(1, "Mending", "Heals for ${172*$<mult>}.", "", "$mult1=${1.0}\n"
                + "$mult=$?s17836[${1.25}][${$<mult1>*1.5}]", 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(effect(0, 1)));

        assertThat(resolver(spell).description(spell)).isEqualTo("Heals for 258.");
    }

    @Test
    void pluralsFollowTheNumberBefore() {
        SpellInfo spell = spell(1, "Lasts $s1 $lminute:minutes; and $s2 $lpoint:points;.", 0, effect(0, 1),
                effect(1, 3));

        assertThat(resolver(spell).description(spell)).isEqualTo("Lasts 1 minute and 3 points.");
    }

    @Test
    void auraValuesComeFromTheSpell() {
        SpellInfo spell = spell(1, "Up to $u stacks, $n charges, $h% chance, $i targets, every $proccooldown sec.",
                0, effect(0, 1));

        assertThat(resolver(spell).description(spell))
                .isEqualTo("Up to 3 stacks, 2 charges, 25% chance, 5 targets, every 6 sec.");
    }

    @Test
    void unknownTokensAndMarkupAreHandled() {
        SpellInfo spell = spell(1, "|cFFFFFFFFBold|r text with $99999d left as written.", 0, effect(0, 1));

        assertThat(resolver(spell).description(spell)).isEqualTo("Bold text with $99999d left as written.");
    }
}
