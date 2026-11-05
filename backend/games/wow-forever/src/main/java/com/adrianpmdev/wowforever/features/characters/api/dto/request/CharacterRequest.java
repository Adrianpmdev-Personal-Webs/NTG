package com.adrianpmdev.wowforever.features.characters.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * A character of a player: its name. Everything else about it is read from the official armory.
 *
 * @param ownerId     account that owns it, which only an admin can choose; null keeps the current owner, or the caller
 *                    on a new character
 * @param name        name and surname in the game: words of letters joined by a space, a hyphen or an apostrophe
 *
 * @author adrianpmdev
 */
public record CharacterRequest(
        UUID ownerId,
        @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = "\\p{L}+(?:[ '\\u2019-]\\p{L}+)*") String name) {
}
