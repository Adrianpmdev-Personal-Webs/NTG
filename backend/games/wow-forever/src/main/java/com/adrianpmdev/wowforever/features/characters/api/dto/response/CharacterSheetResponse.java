package com.adrianpmdev.wowforever.features.characters.api.dto.response;

import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;

import java.util.UUID;

/**
 * A character with its whole sheet from the armory.
 *
 * @param id        character id
 * @param name      name
 * @param owner     account that owns it
 * @param armoryUrl its page in the official armory
 * @param status    FOUND, NOT_FOUND when the armory does not show it (below level 10, or a wrong name), or FAILED
 *                  when the armory could not be read
 * @param sheet     what the armory says, or null when not found
 *
 * @author adrianpmdev
 */
public record CharacterSheetResponse(UUID id, String name, CharactersResponse.Owner owner, String armoryUrl,
                                     String status, ArmorySheet sheet) {
}
