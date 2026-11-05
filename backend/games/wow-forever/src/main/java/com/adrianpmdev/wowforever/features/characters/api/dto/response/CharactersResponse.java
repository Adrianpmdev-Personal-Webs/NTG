package com.adrianpmdev.wowforever.features.characters.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * The characters of the group.
 *
 * @param limit      characters an account can have
 * @param owners     accounts an admin can give a character to; empty for everybody else
 * @param characters every character, sorted by owner and name
 *
 * @author adrianpmdev
 */
public record CharactersResponse(int limit, List<Owner> owners, List<Character> characters) {

    /**
     * An account.
     *
     * @param id       account id
     * @param username name of the account, or null when the account no longer exists
     */
    public record Owner(UUID id, String username) {
    }

    /**
     * A character.
     *
     * @param id          character id
     * @param owner       account that owns it
     * @param name        name in the game
     * @param classSlug   class, or null while the armory does not have it
     * @param raceSlug    race, or null while the armory does not have it
     * @param level       level, or null while the armory does not have it
     * @param professions professions
     * @param notes       free notes, or null
     * @param armoryUrl   where the official armory shows it
     * @param canEdit     whether the caller may change it
     */
    public record Character(UUID id, Owner owner, String name, String classSlug, String raceSlug, Integer level,
                            List<Profession> professions, String notes, String armoryUrl, boolean canEdit) {
    }

    /**
     * A profession of a character.
     *
     * @param slug     profession
     * @param skill    skill points, or null
     * @param maxSkill skill points it can reach for now, or null
     */
    public record Profession(String slug, Integer skill, Integer maxSkill) {
    }
}
