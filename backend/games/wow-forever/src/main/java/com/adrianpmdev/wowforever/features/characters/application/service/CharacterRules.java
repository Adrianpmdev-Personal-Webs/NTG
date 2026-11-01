package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.error.ApiErrors;
import com.adrianpmdev.wowforever.features.characters.api.dto.request.CharacterRequest;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What a character can be, according to the game data the screens show: an existing class, a race that can play it, a
 * level the game has and professions that exist, with two primary ones at most. Only asked when a character is saved.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class CharacterRules {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SECONDARY_GROUP = "secondary";
    private static final int MAX_PRIMARY_PROFESSIONS = 2;

    private final GameDataService gameDataService;

    /**
     * Checks a character.
     *
     * @param request the character
     * @throws org.springframework.web.server.ResponseStatusException 400 with the reason, in Spanish
     */
    public void check(CharacterRequest request) {
        checkClassAndRace(request);
        checkLevel(request);
        checkProfessions(request);
    }

    private void checkClassAndRace(CharacterRequest request) {
        String className = null;

        for (JsonNode gameClass : read(gameDataService.getIndex()).path("classes")) {
            if (gameClass.path("slug").asText().equals(request.classSlug())) {
                className = gameClass.path("name").asText();
            }
        }

        if (className == null) {
            throw ApiErrors.badRequest("Esa clase no existe");
        }

        for (JsonNode race : read(gameDataService.getRacials()).path("races")) {
            if (race.path("slug").asText().equals(request.raceSlug())) {
                for (JsonNode classOfRace : race.path("classNames")) {
                    if (classOfRace.asText().equals(className)) {
                        return;
                    }
                }

                throw ApiErrors.badRequest("Esa raza no puede ser de esa clase");
            }
        }

        throw ApiErrors.badRequest("Esa raza no existe");
    }

    private void checkLevel(CharacterRequest request) {
        int maxLevel = gameDataService.getConfig().talents().lastPointLevel();

        if (request.level() > maxLevel) {
            throw ApiErrors.badRequest("El nivel máximo es " + maxLevel);
        }
    }

    private void checkProfessions(CharacterRequest request) {
        Map<String, String> groupBySlug = new HashMap<>();

        for (JsonNode group : read(gameDataService.getProfessions()).path("groups")) {
            for (JsonNode profession : group.path("professions")) {
                groupBySlug.put(profession.path("slug").asText(), group.path("id").asText());
            }
        }

        Set<String> seen = new HashSet<>();
        int primary = 0;

        for (CharacterRequest.Profession profession : request.professions()) {
            String group = groupBySlug.get(profession.slug());

            if (group == null) {
                throw ApiErrors.badRequest("Esa profesión no existe");
            }

            if (!seen.add(profession.slug())) {
                throw ApiErrors.badRequest("Hay una profesión repetida");
            }

            if (!SECONDARY_GROUP.equals(group)) {
                primary++;
            }
        }

        if (primary > MAX_PRIMARY_PROFESSIONS) {
            throw ApiErrors.badRequest("Un personaje solo puede tener dos profesiones principales");
        }
    }

    private static JsonNode read(String json) {
        try {
            return JSON.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The game data could not be read", exception);
        }
    }
}
