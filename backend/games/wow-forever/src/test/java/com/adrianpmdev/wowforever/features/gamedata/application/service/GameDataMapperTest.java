package com.adrianpmdev.wowforever.features.gamedata.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The cooldown read from an ability text as the export writes it, with texts of real racials.
 *
 * @author adrianpmdev
 */
class GameDataMapperTest {

    @Test
    void readsTheCooldownOfActiveAbilities() {
        assertThat(GameDataMapper.cooldownOf("Instant, 2 min cooldown. Increases Attack Power by 10% for 15 sec."))
                .isEqualTo("2 min");
        assertThat(GameDataMapper.cooldownOf("5 yd range, Instant, 2 min cooldown. When activated, regenerates 7%."))
                .isEqualTo("2 min");
        assertThat(GameDataMapper.cooldownOf("350 Mana, 30 yd range, Channeled, 30 sec cooldown. Rains starshards."))
                .isEqualTo("30 sec");
        assertThat(GameDataMapper.cooldownOf("Instant, 10 sec cooldown. Activate to slip into the shadows."))
                .isEqualTo("10 sec");
    }

    @Test
    void passiveAndUncooledAbilitiesHaveNone() {
        assertThat(GameDataMapper.cooldownOf("Damage dealt versus Beasts increased by 5%.")).isNull();
        assertThat(GameDataMapper.cooldownOf("Instant. Allows the dwarf to sense nearby treasure.")).isNull();
        assertThat(GameDataMapper.cooldownOf("Reduces the cooldown of Shadowmeld by 2 min cooldown, of course.")).isNull();
        assertThat(GameDataMapper.cooldownOf("")).isNull();
    }
}
