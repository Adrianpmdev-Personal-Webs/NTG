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
 * What WoW Forever shows on the profile of a player: one card per character with its race, class and icon, and bars for
 * its level and professions. While the game data cannot be read, the characters still show, with the slugs instead of
 * the names.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class CharacterProfileBlock implements ProfileBlockProvider {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_SKILL = 300;

    private final GameCharacterRepository characterRepository;
    private final GameDataService gameDataService;

    private record Item(String name, String icon) {
    }

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

        Map<String, Item> classes = items(read(gameDataService::getIndex).path("classes"));
        Map<String, Item> races = items(read(gameDataService::getRacials).path("races"));
        Map<String, Item> professions = professionItems();
        int maxLevel = gameDataService.getConfig().talents().lastPointLevel();

        List<ProfileBlock.Card> cards = characters.stream()
                .map(character -> cardOf(character, classes, races, professions, maxLevel))
                .toList();
        int highestLevel = characters.stream().mapToInt(GameCharacter::getLevel).max().orElse(0);
        String count = characters.size() == 1 ? "1 personaje" : characters.size() + " personajes";

        return Optional.of(new ProfileBlock(List.of(count, "Nivel máximo " + highestLevel),
                List.of(new ProfileBlock.Section("Personajes", List.of(), List.of(), cards))));
    }

    private static ProfileBlock.Card cardOf(GameCharacter character, Map<String, Item> classes, Map<String, Item> races,
                                            Map<String, Item> professions, int maxLevel) {
        Item gameClass = itemOf(classes, character.getClassSlug());
        List<ProfileBlock.Bar> bars = new ArrayList<>();

        bars.add(new ProfileBlock.Bar("Nivel", (int) character.getLevel(), maxLevel, null));

        for (JsonNode known : character.getProfessions()) {
            Item profession = itemOf(professions, known.path("slug").asText());
            JsonNode skill = known.path("skill");

            bars.add(new ProfileBlock.Bar(profession.name(), skill.isNumber() ? skill.asInt() : null, MAX_SKILL,
                    profession.icon()));
        }

        return new ProfileBlock.Card(character.getName(),
                itemOf(races, character.getRaceSlug()).name() + " " + gameClass.name(), gameClass.icon(), bars);
    }

    private static Item itemOf(Map<String, Item> items, String slug) {
        return items.getOrDefault(slug, new Item(slug, null));
    }

    private Map<String, Item> professionItems() {
        Map<String, Item> items = new HashMap<>();

        for (JsonNode group : read(gameDataService::getProfessions).path("groups")) {
            items.putAll(items(group.path("professions")));
        }

        return items;
    }

    private static Map<String, Item> items(JsonNode list) {
        Map<String, Item> items = new HashMap<>();

        for (JsonNode item : list) {
            items.put(item.path("slug").asText(), new Item(item.path("name").asText(),
                    item.path("icon").asText(null)));
        }

        return items;
    }

    private static JsonNode read(Supplier<String> source) {
        try {
            return JSON.readTree(source.get());
        } catch (JsonProcessingException | ResponseStatusException exception) {
            return JSON.createObjectNode();
        }
    }
}
