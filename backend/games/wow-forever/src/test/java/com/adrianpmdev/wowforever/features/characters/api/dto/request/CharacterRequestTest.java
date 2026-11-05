package com.adrianpmdev.wowforever.features.characters.api.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * Character names include a surname, so they are longer than the names of the original game.
 *
 * @author adrianpmdev
 */
class CharacterRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "Jo",
            "Thrall",
            "Jaina Valiente",
            "Mak'gora Ojo-de-Fuego Rojo",
            "Ñandú Álvarez",
            "Aaaaaaaaaaaaaaaaaaa Bbbbbbbbbbbbbbbbbbbb"
    })
    void namesWithSurnameAreAccepted(String name) {
        assertThat(VALIDATOR.validate(request(name))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "J",
            "Jaina  Valiente",
            "Jaina ",
            " Jaina",
            "Jaina 2",
            "Jaina-",
            "Aaaaaaaaaaaaaaaaaaaa Bbbbbbbbbbbbbbbbbbbb"
    })
    void otherNamesAreRefused(String name) {
        assertThat(VALIDATOR.validate(request(name))).isNotEmpty();
    }

    private static CharacterRequest request(String name) {
        return new CharacterRequest(null, name);
    }
}
