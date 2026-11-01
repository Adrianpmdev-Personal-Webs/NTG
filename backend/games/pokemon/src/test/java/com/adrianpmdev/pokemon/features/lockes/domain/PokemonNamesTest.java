package com.adrianpmdev.pokemon.features.lockes.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * However a player types a Pokémon name, it is stored the same way.
 *
 * @author adrianpmdev
 */
class PokemonNamesTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "pikachu|Pikachu",
            "PIKACHU|Pikachu",
            "  pIkAcHu  |Pikachu",
            "mr.   mime|Mr. Mime",
            "ho-oh|Ho-Oh",
            "porygon-z|Porygon-Z",
            "farfetch'd|Farfetch'd",
            "type: null|Type: Null",
            "nidoran♀|Nidoran♀",
            "porygon2|Porygon2"
    })
    void namesAreWrittenTheSameWay(String typed, String stored) {
        assertThat(PokemonNames.normalize(typed)).isEqualTo(stored);
    }
}
