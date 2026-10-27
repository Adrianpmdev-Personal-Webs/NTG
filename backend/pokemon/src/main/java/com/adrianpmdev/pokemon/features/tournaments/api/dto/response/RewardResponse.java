package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.util.UUID;

/**
 * A reward of a tournament.
 *
 * @param id        reward id
 * @param sortOrder display order
 * @param name      reward name
 * @param chosenBy  tournament player who picked it, or null
 *
 * @author adrianpmdev
 */
public record RewardResponse(UUID id, int sortOrder, String name, UUID chosenBy) {
}
