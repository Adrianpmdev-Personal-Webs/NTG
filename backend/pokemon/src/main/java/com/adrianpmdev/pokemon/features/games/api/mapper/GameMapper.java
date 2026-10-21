package com.adrianpmdev.pokemon.features.games.api.mapper;

import com.adrianpmdev.pokemon.features.games.api.dto.request.GameCreateRequest;
import com.adrianpmdev.pokemon.features.games.api.dto.request.GameUpdateRequest;
import com.adrianpmdev.pokemon.features.games.api.dto.response.GameResponse;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity.Game;

/**
 * Mapping utility for converting game entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class GameMapper {

    private GameMapper() {
    }

    public static GameResponse toResponse(Game game) {
        return new GameResponse(
                game.getId(),
                game.getName(),
                game.getDescription(),
                game.getImageUrl(),
                game.getCreatedAt()
        );
    }

    public static Game toEntity(GameCreateRequest request) {
        Game game = new Game();
        game.setName(request.name());
        game.setDescription(request.description());
        game.setImageUrl(request.imageUrl());
        return game;
    }

    public static void updateEntity(Game game, GameUpdateRequest request) {
        if (request.name() != null) {
            game.setName(request.name());
        }

        if (request.description() != null) {
            game.setDescription(request.description());
        }

        if (request.imageUrl() != null) {
            game.setImageUrl(request.imageUrl());
        }
    }
}