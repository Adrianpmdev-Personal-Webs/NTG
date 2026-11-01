package com.adrianpmdev.pokemon.features.lockes.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * A Pokémon a player lost. The name accepts what Pokémon names use, such as {@code Mr. Mime}, {@code Farfetch'd},
 * {@code Type: Null} or {@code Nidoran♀}.
 *
 * @param playerId player who lost it
 * @param pokemon  name of the Pokémon
 *
 * @author adrianpmdev
 */
public record LockeDeathRequest(
        @NotNull UUID playerId,
        @NotBlank @Size(max = 30) @Pattern(regexp = "[\\p{L}\\p{N} .:'\\u2019\\u2640\\u2642-]+") String pokemon) {
}
