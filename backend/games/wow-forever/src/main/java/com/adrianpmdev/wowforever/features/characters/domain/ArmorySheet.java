package com.adrianpmdev.wowforever.features.characters.domain;

import java.time.Instant;
import java.util.List;

/**
 * A character as the official armory shows it, in English like the game data: what the web needs to draw its sheet.
 * Nothing of it is stored; it is read from the armory when somebody looks.
 *
 * @param name             full name, as written in the game
 * @param level            level
 * @param className        class
 * @param classSlug        class, as the game data names it
 * @param raceName         race
 * @param raceSlug         race, as the game data names it
 * @param faction          {@code ALLIANCE} or {@code HORDE}
 * @param gender           gender
 * @param guild            guild, or null
 * @param averageItemLevel average item level
 * @param lastUpdated      when the armory last changed it, or null
 * @param images           pictures of the character
 * @param professions      professions
 * @param gear             equipped items
 * @param stats            statistics, by group
 * @param specs            talent trees with the points spent in them
 * @param honorableKills   honorable kills
 *
 * @author adrianpmdev
 */
public record ArmorySheet(String name, int level, String className, String classSlug, String raceName,
                          String raceSlug, String faction, String gender, String guild, int averageItemLevel,
                          Instant lastUpdated, Images images, List<Profession> professions, List<Item> gear,
                          List<StatGroup> stats, List<Spec> specs, int honorableKills) {

    /**
     * Pictures of the character, from Blizzard's render host.
     *
     * @param avatar     the face
     * @param render     the whole character, without background
     * @param background the background of its race
     */
    public record Images(String avatar, String render, String background) {
    }

    /**
     * A profession.
     *
     * @param name     name
     * @param slug     name as the game data names it
     * @param skill    skill points
     * @param maxSkill skill points it can reach for now
     * @param icon     icon
     */
    public record Profession(String name, String slug, int skill, int maxSkill, String icon) {
    }

    /**
     * An equipped item.
     *
     * @param slot     slot, such as {@code MAIN_HAND}
     * @param slotName name of the slot
     * @param name     name
     * @param quality  quality, such as {@code UNCOMMON}
     * @param icon     icon
     * @param lines    its tooltip, as the armory writes it
     */
    public record Item(String slot, String slotName, String name, String quality, String icon, List<Line> lines) {
    }

    /**
     * A line of a tooltip.
     *
     * @param left  text on the left
     * @param right text on the right, or null
     * @param color colour as {@code #rrggbb}, or null for the usual one
     */
    public record Line(String left, String right, String color) {
    }

    /**
     * A group of statistics.
     *
     * @param name  name, such as Attributes
     * @param stats statistics
     */
    public record StatGroup(String name, List<Stat> stats) {
    }

    /**
     * A statistic.
     *
     * @param name  name
     * @param value value, written as the armory shows it
     */
    public record Stat(String name, String value) {
    }

    /**
     * A talent tree.
     *
     * @param name    name
     * @param icon    icon
     * @param points  points spent in it
     * @param talents talents learned in it
     */
    public record Spec(String name, String icon, int points, List<Talent> talents) {
    }

    /**
     * A learned talent.
     *
     * @param name        name
     * @param rank        rank
     * @param icon        icon
     * @param cast        how it is used, such as Passive, or null
     * @param description what it does
     */
    public record Talent(String name, int rank, String icon, String cast, String description) {
    }
}
