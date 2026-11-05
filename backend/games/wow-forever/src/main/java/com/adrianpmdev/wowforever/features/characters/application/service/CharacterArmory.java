package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryClient;
import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryMatch;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fills a character from the official armory. The search is small and tells when the armory last changed the
 * character, so its whole page is only read when it has changed.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class CharacterArmory {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ArmoryClient armoryClient;

    /** A profession as it is kept in the character. */
    private record KnownProfession(String slug, int skill, int maxSkill) {
    }

    /**
     * What the armory says of a character, if there is anything new.
     *
     * @param character the character
     * @return the data to apply, or empty when the armory does not have it or nothing has changed
     * @throws IOException when the armory cannot be read
     */
    public Optional<ArmoryCharacter> changesOf(GameCharacter character) throws IOException {
        Optional<ArmoryMatch> match = armoryClient.search(character.getName());

        if (match.isEmpty() || (match.get().slug().equals(character.getArmorySlug())
                && Objects.equals(match.get().lastUpdated(), character.getArmoryUpdatedAt()))) {
            return Optional.empty();
        }

        return Optional.of(armoryClient.read(match.get().slug()));
    }

    /**
     * Writes the data of the armory on a character.
     *
     * @param character the character
     * @param armory    what the armory says
     */
    public void apply(GameCharacter character, ArmoryCharacter armory) {
        List<KnownProfession> professions = armory.professions().stream()
                .map(known -> new KnownProfession(GameDataService.slugOf(known.name()), known.skill(), known.maxSkill()))
                .toList();

        character.setName(armory.name());
        character.setClassSlug(GameDataService.slugOf(armory.className()));
        character.setRaceSlug(GameDataService.slugOf(armory.raceName()));
        character.setLevel((short) armory.level());
        character.setProfessions(JSON.valueToTree(professions));
        character.setArmorySlug(armory.slug());
        character.setArmoryUpdatedAt(armory.lastUpdated());
    }

    /**
     * Forgets what the armory said, because the character is now another one.
     *
     * @param character the character
     */
    public void clear(GameCharacter character) {
        character.setClassSlug(null);
        character.setRaceSlug(null);
        character.setLevel(null);
        character.setProfessions(JSON.createArrayNode());
        character.setArmorySlug(null);
        character.setArmoryUpdatedAt(null);
    }
}
