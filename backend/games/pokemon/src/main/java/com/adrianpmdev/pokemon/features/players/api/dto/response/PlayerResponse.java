package com.adrianpmdev.pokemon.features.players.api.dto.response;

import java.util.UUID;

/**
 * A player.
 *
 * @param id     player id
 * @param name   display name
 * @param userId linked platform account; only sent to admins, null for everybody else
 *
 * @author adrianpmdev
 */
public record PlayerResponse(UUID id, String name, UUID userId) {
}
