package com.adrianpmdev.pokemon.shared.dto;

import java.util.UUID;

/**
 * Short reference to another resource (a game, a randomlocke, a player...).
 *
 * @param id   resource id
 * @param name display name
 *
 * @author adrianpmdev
 */
public record RefResponse(UUID id, String name) {
}
