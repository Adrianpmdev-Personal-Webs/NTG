package com.adrianpmdev.wowforever.features.characters.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * The characters of the group, without what the armory says (see CharacterSummary and CharacterSheetResponse).
 *
 * @param myUserId   account of the caller
 * @param limit      characters an account can have
 * @param owners     accounts an admin can give a character to; empty for everybody else
 * @param characters every character, sorted by owner and name
 *
 * @author adrianpmdev
 */
public record CharactersResponse(UUID myUserId, int limit, List<Owner> owners, List<Character> characters) {

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
     * @param id        character id
     * @param owner     account that owns it
     * @param name      name in the game
     * @param armoryUrl its page in the official armory
     * @param canEdit   whether the caller may change it
     */
    public record Character(UUID id, Owner owner, String name, String armoryUrl, boolean canEdit) {
    }
}
