package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.wowforever.features.characters.api.dto.request.CharacterRequest;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A character can only be what the game data allows.
 *
 * @author adrianpmdev
 */
class CharacterRulesTest {

    private final GameDataService gameData = mock(GameDataService.class);
    private final CharacterRules rules = new CharacterRules(gameData);

    @BeforeEach
    void setUp() {
        GameDataConfig config = mock(GameDataConfig.class, Answers.RETURNS_DEEP_STUBS);
        when(config.talents().lastPointLevel()).thenReturn(60);
        when(gameData.getConfig()).thenReturn(config);
        when(gameData.getIndex()).thenReturn("""
                {"classes":[{"slug":"warrior","name":"Warrior"},{"slug":"mage","name":"Mage"}]}""");
        when(gameData.getRacials()).thenReturn("""
                {"races":[{"slug":"orc","classNames":["Warrior"]},{"slug":"human","classNames":["Warrior","Mage"]}]}""");
        when(gameData.getProfessions()).thenReturn("""
                {"groups":[
                  {"id":"crafting","professions":[{"slug":"alchemy"},{"slug":"tailoring"}]},
                  {"id":"gathering","professions":[{"slug":"mining"}]},
                  {"id":"secondary","professions":[{"slug":"cooking"},{"slug":"fishing"},{"slug":"first-aid"}]}
                ]}""");
    }

    @Test
    void aValidCharacterPasses() {
        assertThatCode(() -> rules.check(character("mage", "human", 60,
                "alchemy", "mining", "cooking", "fishing", "first-aid"))).doesNotThrowAnyException();
    }

    @Test
    void aCharacterWithoutProfessionsPasses() {
        assertThatCode(() -> rules.check(character("warrior", "orc", 1))).doesNotThrowAnyException();
    }

    @Test
    void anUnknownClassIsRefused() {
        assertRefused(character("paladin", "human", 60));
    }

    @Test
    void anUnknownRaceIsRefused() {
        assertRefused(character("warrior", "murloc", 60));
    }

    @Test
    void aRaceThatCannotPlayTheClassIsRefused() {
        assertRefused(character("mage", "orc", 60));
    }

    @Test
    void aLevelOverTheMaximumIsRefused() {
        assertRefused(character("warrior", "orc", 61));
    }

    @Test
    void anUnknownProfessionIsRefused() {
        assertRefused(character("warrior", "orc", 60, "cheating"));
    }

    @Test
    void aRepeatedProfessionIsRefused() {
        assertRefused(character("warrior", "orc", 60, "cooking", "cooking"));
    }

    @Test
    void aThirdPrimaryProfessionIsRefused() {
        assertRefused(character("warrior", "orc", 60, "alchemy", "tailoring", "mining"));
    }

    private void assertRefused(CharacterRequest request) {
        assertThatThrownBy(() -> rules.check(request)).isInstanceOf(ResponseStatusException.class);
    }

    private static CharacterRequest character(String classSlug, String raceSlug, int level, String... professions) {
        List<CharacterRequest.Profession> list = java.util.Arrays.stream(professions)
                .map(slug -> new CharacterRequest.Profession(slug, null))
                .toList();

        return new CharacterRequest(null, "Thrall", classSlug, raceSlug, level, list, null);
    }
}
