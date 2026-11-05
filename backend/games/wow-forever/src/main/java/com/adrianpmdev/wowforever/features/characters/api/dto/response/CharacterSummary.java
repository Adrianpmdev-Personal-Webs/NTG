package com.adrianpmdev.wowforever.features.characters.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * What the armory says of a character, in short, for the list.
 *
 * @param id          character id
 * @param status      FOUND, NOT_FOUND when the armory does not show it (below level 10, or a wrong name), or FAILED
 *                    when the armory could not be read
 * @param level       level, or null when not found
 * @param className   class, or null when not found
 * @param classSlug   class as the game data names it, or null when not found
 * @param raceName    race, or null when not found
 * @param raceSlug    race as the game data names it, or null when not found
 * @param professions professions; empty when not found
 *
 * @author adrianpmdev
 */
public record CharacterSummary(UUID id, String status, Integer level, String className, String classSlug,
                               String raceName, String raceSlug, List<Profession> professions) {

    /**
     * A profession.
     *
     * @param slug     name as the game data names it
     * @param name     name
     * @param skill    skill points
     * @param maxSkill skill points it can reach for now
     * @param icon     icon
     */
    public record Profession(String slug, String name, int skill, int maxSkill, String icon) {
    }
}
