package com.adrianpmdev.pokemon;

import com.adrianpmdev.platform.module.GameModule;
import com.adrianpmdev.platform.module.GameModule.PlayerWrite;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * Declares the Pokémon module to the platform. Players with the tag read everything; the only writes open to them
 * are the team pictures, and the module then accepts only the player of that tournament entry (or an admin).
 *
 * @author adrianpmdev
 */
@Configuration
public class PokemonModule {

    private static final String TEAM_IMAGE = "/tournament-players/*/team-image";

    /**
     * Declaration of the module.
     *
     * @return the game module
     */
    @Bean
    GameModule pokemonGameModule() {
        return new GameModule("pokemon", "/api/v1/pokemon", List.of(
                new PlayerWrite(HttpMethod.PUT, TEAM_IMAGE),
                new PlayerWrite(HttpMethod.DELETE, TEAM_IMAGE)
        ));
    }
}
