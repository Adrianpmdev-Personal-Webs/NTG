package com.adrianpmdev.wowforever.features.gamedata.domain;

import java.util.List;

/**
 * What a spell tooltip can show, as the game client stores it. Texts keep their {@code $} tokens; SpellTextResolver
 * turns them into numbers.
 *
 * @param id              spell id
 * @param name            name
 * @param description     description with tokens, or an empty text
 * @param auraDescription text of the buff or debuff it applies, with tokens, or an empty text
 * @param variables       definitions of its named values ({@code $name=expression}, one per line), or an empty text
 * @param durationMs      duration in milliseconds, -1 for "until cancelled", 0 for none
 * @param spellLevel      level its values are based on
 * @param maxLevel        level its values stop growing at, or 0 for no limit
 * @param rangeYards      longest range in yards, 0 for none
 * @param stacks          most applications of its aura, 0 for none
 * @param charges         times its aura can trigger, 0 for no limit
 * @param procChance      chance of its aura to trigger, in percent
 * @param procCooldownMs  shortest time between two triggers, in milliseconds
 * @param maxTargets      most targets it hits, 0 for no limit
 * @param effects         its effects, by effect index
 *
 * @author adrianpmdev
 */
public record SpellInfo(
        int id,
        String name,
        String description,
        String auraDescription,
        String variables,
        int durationMs,
        int spellLevel,
        int maxLevel,
        double rangeYards,
        int stacks,
        int charges,
        int procChance,
        int procCooldownMs,
        int maxTargets,
        List<Effect> effects
) {

    /** Duration of a spell that lasts until it is cancelled. */
    public static final int UNTIL_CANCELLED = -1;

    /**
     * The same spell with another description, for a talent that shows its own text with the spell's values.
     *
     * @param text description with tokens
     * @return a copy of the spell
     */
    public SpellInfo withDescription(String text) {
        return new SpellInfo(id, name, text, auraDescription, variables, durationMs, spellLevel, maxLevel, rangeYards,
                stacks, charges, procChance, procCooldownMs, maxTargets, effects);
    }

    /**
     * The effect with an index, starting at 0.
     *
     * @param index effect index
     * @return the effect, or null when the spell has none with that index
     */
    public Effect effect(int index) {
        return effects.stream().filter(effect -> effect.index() == index).findFirst().orElse(null);
    }

    /**
     * One effect of a spell.
     *
     * @param index          position, starting at 0 ({@code $s1} is index 0)
     * @param basePoints     base value
     * @param pointsPerLevel value gained per level above the spell level
     * @param variance       spread of the value: 0.2 means from 10% below to 10% above
     * @param periodMs       time between two ticks of a periodic effect, in milliseconds
     * @param amplitude      value multiplier ({@code $e})
     * @param chainTargets   targets it jumps to ({@code $x})
     * @param radiusYards    radius in yards ({@code $a})
     */
    public record Effect(int index, double basePoints, double pointsPerLevel, double variance, int periodMs,
                         double amplitude, int chainTargets, double radiusYards) {
    }
}
