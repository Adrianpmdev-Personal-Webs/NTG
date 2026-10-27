package com.adrianpmdev.pokemon.features.games.api.dto.response;

import java.util.UUID;

/**
 * A game.
 *
 * @param id          game id
 * @param name        game name
 * @param description description
 * @param imageUrl    cover picture
 * @param lockeCount  randomlockes with at least one tournament in this game
 *
 * @author adrianpmdev
 */
public record GameResponse(UUID id, String name, String description, String imageUrl, int lockeCount) {
}
