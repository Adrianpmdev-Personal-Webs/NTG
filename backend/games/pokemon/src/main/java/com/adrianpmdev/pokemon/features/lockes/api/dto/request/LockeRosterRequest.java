package com.adrianpmdev.pokemon.features.lockes.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Fixed roster of a randomlocke. New tournaments start with these players.
 *
 * @param playerIds players of the randomlocke
 *
 * @author adrianpmdev
 */
public record LockeRosterRequest(@NotNull @Size(max = 64) List<@NotNull UUID> playerIds) {
}
