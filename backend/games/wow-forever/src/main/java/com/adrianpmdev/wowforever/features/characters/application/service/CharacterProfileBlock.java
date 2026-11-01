package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.platform.profile.ProfileBlockProvider;
import com.adrianpmdev.wowforever.WowForeverModule;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * What WoW Forever shows on the profile of a player: their characters with their level, race, class and professions.
 * While the game data cannot be read, the characters still show, with the slugs instead of the names.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class CharacterProfileBlock implements ProfileBlockProvider {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SEPARATOR = " · ";

    private final GameCharacterRepository characterRepository;
    private final GameDataService gameDataService;

    @Override
    public String gameKey() {
        return WowForeverModule.TAG;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfileBlock> block(UUID userId) {
        List<GameCharacter> characters = characterRepository.findByUserIdOrderByNameAsc(userId);

        if (characters.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> classes = names(gameDataService::getIndex, "classes");
        Map<String, String> races = names(gameDataService::getRacials, "races");
        Map<String, String> professions = professionNames();

        List<ProfileBlock.Entry> entries = characters.stream()
                .map(character -> new ProfileBlock.Entry(character.getName(),
                        detailOf(character, classes, races, professions)))
                .toList();

        return Optional.of(new ProfileBlock(
                List.of(new ProfileBlock.Stat("Personajes", "" + characters.size())), entries));
    }

    private static String detailOf(GameCharacter character, Map<String, String> classes, Map<String, String> races,
                                   Map<String, String> professions) {
        StringBuilder detail = new StringBuilder("Nivel ").append(character.getLevel()).append(SEPARATOR)
                .append(races.getOrDefault(character.getRaceSlug(), character.getRaceSlug())).append(' ')
                .append(classes.getOrDefault(character.getClassSlug(), character.getClassSlug()));
        List<String> known = new ArrayList<>();

        for (JsonNode profession : character.getProfessions()) {
            String slug = profession.path("slug").asText();
            JsonNode skill = profession.path("skill");

            known.add(professions.getOrDefault(slug, slug) + (skill.isNumber() ? " " + skill.asInt() : ""));
        }

        if (!known.isEmpty()) {
            detail.append(SEPARATOR).append(String.join(", ", known));
        }

        return detail.toString();
    }

    private Map<String, String> professionNames() {
        Map<String, String> names = new HashMap<>();

        for (JsonNode group : read(gameDataService::getProfessions).path("groups")) {
            collect(group.path("professions"), names);
        }

        return names;
    }

    private static Map<String, String> names(Supplier<String> source, String field) {
        Map<String, String> names = new HashMap<>();

        collect(read(source).path(field), names);

        return names;
    }

    private static void collect(JsonNode list, Map<String, String> names) {
        for (JsonNode item : list) {
            names.put(item.path("slug").asText(), item.path("name").asText());
        }
    }

    private static JsonNode read(Supplier<String> source) {
        try {
            return JSON.readTree(source.get());
        } catch (JsonProcessingException | ResponseStatusException exception) {
            return JSON.createObjectNode();
        }
    }
}
