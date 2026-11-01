package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What the profile tells about the characters of a player.
 *
 * @author adrianpmdev
 */
class CharacterProfileBlockTest {

    private static final UUID ACCOUNT = UUID.randomUUID();

    private final GameCharacterRepository repository = mock(GameCharacterRepository.class);
    private final GameDataService gameData = mock(GameDataService.class);
    private final CharacterProfileBlock provider = new CharacterProfileBlock(repository, gameData);

    @Test
    void aPlayerWithoutCharactersHasNothing() {
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of());

        assertThat(provider.block(ACCOUNT)).isEmpty();
    }

    @Test
    void charactersAreToldWithTheNamesOfTheGameData() throws Exception {
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of(character()));
        when(gameData.getIndex()).thenReturn("{\"classes\":[{\"slug\":\"mage\",\"name\":\"Mage\"}]}");
        when(gameData.getRacials()).thenReturn("{\"races\":[{\"slug\":\"human\",\"name\":\"Human\"}]}");
        when(gameData.getProfessions()).thenReturn("""
                {"groups":[{"professions":[{"slug":"alchemy","name":"Alchemy"}]},
                           {"professions":[{"slug":"cooking","name":"Cooking"}]}]}""");

        ProfileBlock block = provider.block(ACCOUNT).orElseThrow();

        assertThat(block.stats()).containsExactly(new ProfileBlock.Stat("Personajes", "1"));
        assertThat(block.entries()).containsExactly(
                new ProfileBlock.Entry("Jaina Valiente", "Nivel 60 · Human Mage · Alchemy 300, Cooking"));
    }

    @Test
    void withoutGameDataTheCharactersStillShow() throws Exception {
        ResponseStatusException unavailable = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of(character()));
        when(gameData.getIndex()).thenThrow(unavailable);
        when(gameData.getRacials()).thenThrow(unavailable);
        when(gameData.getProfessions()).thenThrow(unavailable);

        assertThat(provider.block(ACCOUNT).orElseThrow().entries()).containsExactly(
                new ProfileBlock.Entry("Jaina Valiente", "Nivel 60 · human mage · alchemy 300, cooking"));
    }

    private static GameCharacter character() throws Exception {
        GameCharacter character = new GameCharacter();
        character.setUserId(ACCOUNT);
        character.setName("Jaina Valiente");
        character.setClassSlug("mage");
        character.setRaceSlug("human");
        character.setLevel((short) 60);
        character.setProfessions(new ObjectMapper().readTree("""
                [{"slug":"alchemy","skill":300},{"slug":"cooking","skill":null}]"""));
        return character;
    }
}
