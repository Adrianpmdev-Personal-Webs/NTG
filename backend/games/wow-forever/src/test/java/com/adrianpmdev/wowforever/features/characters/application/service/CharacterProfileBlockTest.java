package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The block of WoW Forever on the profile of a player.
 *
 * @author adrianpmdev
 */
class CharacterProfileBlockTest {

    private static final UUID ACCOUNT = UUID.randomUUID();

    private final GameCharacterRepository repository = mock(GameCharacterRepository.class);
    private final ArmoryReader reader = mock(ArmoryReader.class);
    private final GameDataService gameData = mock(GameDataService.class);
    private final CharacterProfileBlock provider = new CharacterProfileBlock(repository, reader, gameData);

    @BeforeEach
    void setUp() {
        GameDataConfig config = mock(GameDataConfig.class, Answers.RETURNS_DEEP_STUBS);
        when(config.talents().lastPointLevel()).thenReturn(60);
        when(gameData.getConfig()).thenReturn(config);
        when(gameData.getIndex()).thenReturn("{\"classes\":[{\"slug\":\"mage\",\"name\":\"Mage\",\"icon\":\"m.jpg\"}]}");
    }

    @Test
    void aPlayerWithoutCharactersHasNothing() {
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of());

        assertThat(provider.block(ACCOUNT)).isEmpty();
    }

    @Test
    void eachCharacterIsACardWithWhatTheArmoryShows() {
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of(character("Jaina Valiente")));
        when(reader.readAll(anyList())).thenReturn(Map.of("Jaina Valiente",
                new ArmoryReader.Result(ArmoryReader.Status.FOUND, sheet("Jaina Valiente", 42))));

        ProfileBlock block = provider.block(ACCOUNT).orElseThrow();

        assertThat(block.summary()).containsExactly("1 personaje", "Nivel máximo 42");
        assertThat(block.sections().getFirst().cards()).containsExactly(new ProfileBlock.Card(
                "Jaina Valiente", "Human Mage", "m.jpg", List.of(
                        new ProfileBlock.Bar("Nivel", 42, 60, null),
                        new ProfileBlock.Bar("Alchemy", 70, 150, "a.jpg"))));
    }

    @Test
    void aCharacterTheArmoryDoesNotShowSaysSoClearly() {
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of(character("Alba"), character("Zul")));
        when(reader.readAll(anyList())).thenReturn(Map.of(
                "Alba", new ArmoryReader.Result(ArmoryReader.Status.NOT_FOUND, null),
                "Zul", new ArmoryReader.Result(ArmoryReader.Status.FAILED, null)));

        ProfileBlock block = provider.block(ACCOUNT).orElseThrow();

        assertThat(block.summary()).containsExactly("2 personajes");
        assertThat(block.sections().getFirst().cards()).containsExactly(
                new ProfileBlock.Card("Alba", "No aparece en la Armería: sale a partir del nivel 10", null, List.of()),
                new ProfileBlock.Card("Zul", "No se ha podido leer la Armería", null, List.of()));
    }

    @Test
    void withoutGameDataTheCardHasNoClassIcon() {
        when(gameData.getIndex()).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        when(repository.findByUserIdOrderByNameAsc(ACCOUNT)).thenReturn(List.of(character("Jaina Valiente")));
        when(reader.readAll(anyList())).thenReturn(Map.of("Jaina Valiente",
                new ArmoryReader.Result(ArmoryReader.Status.FOUND, sheet("Jaina Valiente", 60))));

        ProfileBlock.Card card = provider.block(ACCOUNT).orElseThrow().sections().getFirst().cards().getFirst();

        assertThat(card.iconUrl()).isNull();
        assertThat(card.subtitle()).isEqualTo("Human Mage");
    }

    private static GameCharacter character(String name) {
        GameCharacter character = new GameCharacter();
        character.setUserId(ACCOUNT);
        character.setName(name);
        return character;
    }

    private static ArmorySheet sheet(String name, int level) {
        return new ArmorySheet(name, level, "Mage", "mage", "Human", "human", "ALLIANCE", "Female", null, 10, null,
                new ArmorySheet.Images(null, null, null),
                List.of(new ArmorySheet.Profession("Alchemy", "alchemy", 70, 150, "a.jpg")), List.of(), List.of(),
                List.of(), 0);
    }
}
