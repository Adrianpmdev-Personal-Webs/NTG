package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
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

    @BeforeEach
    void setUp() {
        GameDataConfig config = mock(GameDataConfig.class, Answers.RETURNS_DEEP_STUBS);
        when(config.talents().lastPointLevel()).thenReturn(60);
        when(gameData.getConfig()).thenReturn(config);
    }

    @Test
    void aPlayerWithoutCharactersHasNothing() {
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of());

        assertThat(provider.block(ACCOUNT)).isEmpty();
    }

    @Test
    void eachCharacterIsACardWithTheNamesAndIconsOfTheGameData() throws Exception {
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of(character("Jaina Valiente", 42)));
        when(gameData.getIndex()).thenReturn("{\"classes\":[{\"slug\":\"mage\",\"name\":\"Mage\",\"icon\":\"m.jpg\"}]}");
        when(gameData.getRacials()).thenReturn("{\"races\":[{\"slug\":\"human\",\"name\":\"Human\"}]}");
        when(gameData.getProfessions()).thenReturn("""
                {"groups":[{"professions":[{"slug":"alchemy","name":"Alchemy","icon":"a.jpg"}]},
                           {"professions":[{"slug":"cooking","name":"Cooking","icon":"c.jpg"}]}]}""");

        ProfileBlock block = provider.block(ACCOUNT).orElseThrow();

        assertThat(block.summary()).containsExactly("1 personaje", "Nivel máximo 42");
        assertThat(block.sections().getFirst().cards()).containsExactly(new ProfileBlock.Card(
                "Jaina Valiente", "Human Mage", "m.jpg", List.of(
                        new ProfileBlock.Bar("Nivel", 42, 60, null),
                        new ProfileBlock.Bar("Alchemy", 300, 300, "a.jpg"),
                        new ProfileBlock.Bar("Cooking", null, 300, "c.jpg"))));
    }

    @Test
    void severalCharactersAreCountedAndTheHighestLevelIsTold() throws Exception {
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT))
                .thenReturn(List.of(character("Alba", 10), character("Zul", 60)));
        when(gameData.getIndex()).thenReturn("{}");
        when(gameData.getRacials()).thenReturn("{}");
        when(gameData.getProfessions()).thenReturn("{}");

        assertThat(provider.block(ACCOUNT).orElseThrow().summary()).containsExactly("2 personajes", "Nivel máximo 60");
    }

    @Test
    void withoutGameDataTheCharactersStillShow() throws Exception {
        ResponseStatusException unavailable = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of(character("Jaina Valiente", 60)));
        when(gameData.getIndex()).thenThrow(unavailable);
        when(gameData.getRacials()).thenThrow(unavailable);
        when(gameData.getProfessions()).thenThrow(unavailable);

        ProfileBlock.Card card = provider.block(ACCOUNT).orElseThrow().sections().getFirst().cards().getFirst();

        assertThat(card.subtitle()).isEqualTo("human mage");
        assertThat(card.bars().get(1).label()).isEqualTo("alchemy");
    }

    @Test
    void aCharacterTheArmoryDoesNotShowHasOnlyItsName() throws Exception {
        GameCharacter pending = character("Alba", 1);
        pending.setLevel(null);
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of(pending));
        when(gameData.getIndex()).thenReturn("{}");
        when(gameData.getRacials()).thenReturn("{}");
        when(gameData.getProfessions()).thenReturn("{}");

        ProfileBlock block = provider.block(ACCOUNT).orElseThrow();

        assertThat(block.summary()).containsExactly("1 personaje");
        assertThat(block.sections().getFirst().cards()).containsExactly(
                new ProfileBlock.Card("Alba", "Aún no está en la Armería", null, List.of()));
    }

    private static GameCharacter character(String name, int level) throws Exception {
        GameCharacter character = new GameCharacter();
        character.setUserId(ACCOUNT);
        character.setName(name);
        character.setClassSlug("mage");
        character.setRaceSlug("human");
        character.setLevel((short) level);
        character.setProfessions(new ObjectMapper().readTree("""
                [{"slug":"alchemy","skill":300},{"slug":"cooking","skill":null}]"""));
        return character;
    }
}
