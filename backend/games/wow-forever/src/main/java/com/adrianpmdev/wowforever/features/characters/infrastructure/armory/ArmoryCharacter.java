package com.adrianpmdev.wowforever.features.characters.infrastructure.armory;

import java.time.Instant;
import java.util.List;

/**
 * A character as the armory shows it, in English like the game data.
 *
 * @param slug        its id in the armory
 * @param name        full name, as written in the game
 * @param className   class
 * @param raceName    race
 * @param level       level
 * @param professions professions
 * @param lastUpdated when the armory last changed it, or null when it does not tell
 *
 * @author adrianpmdev
 */
public record ArmoryCharacter(String slug, String name, String className, String raceName, int level,
                              List<Profession> professions, Instant lastUpdated) {

    /**
     * A profession.
     *
     * @param name     name
     * @param skill    skill points
     * @param maxSkill skill points it can reach for now
     */
    public record Profession(String name, int skill, int maxSkill) {
    }
}
