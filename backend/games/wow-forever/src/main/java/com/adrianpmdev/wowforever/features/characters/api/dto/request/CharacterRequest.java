package com.adrianpmdev.wowforever.features.characters.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * A character of a player. The class, the race, the level and the professions are checked against the game data.
 *
 * @param ownerId     account that owns it, which only an admin can choose; null keeps the current owner, or the caller
 *                    on a new character
 * @param name        name and surname in the game: words of letters joined by a space, a hyphen or an apostrophe
 * @param classSlug   class
 * @param raceSlug    race
 * @param level       level
 * @param professions professions
 * @param notes       free notes, optional
 *
 * @author adrianpmdev
 */
public record CharacterRequest(
        UUID ownerId,
        @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = "\\p{L}+(?:[ '\\u2019-]\\p{L}+)*") String name,
        @NotBlank @Size(max = 40) String classSlug,
        @NotBlank @Size(max = 40) String raceSlug,
        @NotNull @Min(1) Integer level,
        @NotNull @Size(max = 5) List<@Valid @NotNull Profession> professions,
        @Size(max = 200) String notes) {

    /**
     * A profession of the character.
     *
     * @param slug  profession
     * @param skill skill points, optional
     */
    public record Profession(@NotBlank @Size(max = 40) String slug, @Min(1) @Max(300) Integer skill) {
    }
}
