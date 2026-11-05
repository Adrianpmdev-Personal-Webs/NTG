package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.platform.profile.ProfileBlockProvider;
import com.adrianpmdev.wowforever.WowForeverModule;
import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * What WoW Forever shows on the profile of a player: one card per character with its race, class and icon, and bars for
 * its level and professions, read from the official armory. A character the armory does not show, or an armory that
 * cannot be read, says so with the name only.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class CharacterProfileBlock implements ProfileBlockProvider {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String NOT_IN_ARMORY = "No aparece en la Armería: sale a partir del nivel 10";
    private static final String NOT_READ = "No se ha podido leer la Armería";

    private final GameCharacterRepository characterRepository;
    private final ArmoryReader armoryReader;
    private final GameDataService gameDataService;

    @Override
    public String gameKey() {
        return WowForeverModule.TAG;
    }

    @Override
    public Optional<ProfileBlock> block(UUID userId) {
        List<GameCharacter> characters = characterRepository.findByUserIdOrderByNameAsc(userId);

        if (characters.isEmpty()) {
            return Optional.empty();
        }

        Map<String, ArmoryReader.Result> results = armoryReader.readAll(
                characters.stream().map(GameCharacter::getName).toList());
        int maxLevel = gameDataService.getConfig().talents().lastPointLevel();

        List<ProfileBlock.Card> cards = characters.stream()
                .map(character -> cardOf(character.getName(), results.get(character.getName()), maxLevel))
                .toList();
        OptionalInt highestLevel = results.values().stream()
                .filter(result -> result.sheet() != null)
                .mapToInt(result -> result.sheet().level())
                .max();
        String count = characters.size() == 1 ? "1 personaje" : characters.size() + " personajes";
        List<String> summary = highestLevel.isPresent()
                ? List.of(count, "Nivel máximo " + highestLevel.getAsInt())
                : List.of(count);

        return Optional.of(new ProfileBlock(summary,
                List.of(new ProfileBlock.Section("Personajes", List.of(), List.of(), cards))));
    }

    private ProfileBlock.Card cardOf(String name, ArmoryReader.Result result, int maxLevel) {
        ArmorySheet sheet = result.sheet();

        if (sheet == null) {
            return new ProfileBlock.Card(name,
                    result.status() == ArmoryReader.Status.NOT_FOUND ? NOT_IN_ARMORY : NOT_READ, null, List.of());
        }

        List<ProfileBlock.Bar> bars = new ArrayList<>();

        bars.add(new ProfileBlock.Bar("Nivel", sheet.level(), maxLevel, null));
        sheet.professions().forEach(known ->
                bars.add(new ProfileBlock.Bar(known.name(), known.skill(), known.maxSkill(), known.icon())));

        return new ProfileBlock.Card(sheet.name(), sheet.raceName() + " " + sheet.className(),
                classIcon(sheet.classSlug()), bars);
    }

    private String classIcon(String classSlug) {
        try {
            for (JsonNode gameClass : JSON.readTree(gameDataService.getIndex()).path("classes")) {
                if (gameClass.path("slug").asText().equals(classSlug)) {
                    return gameClass.path("icon").asText(null);
                }
            }
        } catch (JsonProcessingException | ResponseStatusException exception) {
            return null;
        }

        return null;
    }
}
