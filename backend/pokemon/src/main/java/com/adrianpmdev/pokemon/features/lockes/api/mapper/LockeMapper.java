package com.adrianpmdev.pokemon.features.lockes.api.mapper;

import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeCreateRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeUpdateRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeResponse;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity.Game;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Mapping utility for converting locke entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class LockeMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LockeMapper() {
    }

    public static LockeResponse toResponse(Locke locke) {
        return new LockeResponse(
                locke.getId(),
                getGameId(locke.getGame()),
                locke.getName(),
                locke.getDescription(),
                toObject(locke.getRules()),
                locke.getCreatedAt()
        );
    }

    public static Locke toEntity(LockeCreateRequest request, Game game) {
        Locke locke = new Locke();
        locke.setGame(game);
        locke.setName(request.name());
        locke.setDescription(request.description());
        locke.setRules(toJsonNode(request.rules()));
        return locke;
    }

    public static void updateEntity(Locke locke, LockeUpdateRequest request, Game game) {
        if (game != null) {
            locke.setGame(game);
        }

        if (request.name() != null) {
            locke.setName(request.name());
        }

        if (request.description() != null) {
            locke.setDescription(request.description());
        }

        if (request.rules() != null) {
            locke.setRules(toJsonNode(request.rules()));
        }
    }

    private static JsonNode toJsonNode(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        }

        return OBJECT_MAPPER.valueToTree(value);
    }

    private static Object toObject(JsonNode value) {
        if (value == null) {
            return null;
        }

        return OBJECT_MAPPER.convertValue(value, Object.class);
    }

    private static UUID getGameId(Game game) {
        return game == null ? null : game.getId();
    }
}