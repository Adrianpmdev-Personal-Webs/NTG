package com.adrianpmdev.pokemon;

import com.adrianpmdev.platform.module.GameModule;
import com.adrianpmdev.platform.module.GameModule.PlayerWrite;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * Declares the Pokémon module to the platform. Players with the tag read everything; the only writes open to them
 * are the team pictures and the Pokémon lost in a randomlocke, and the module then accepts only the player they
 * belong to (or an admin).
 *
 * @author adrianpmdev
 */
@Configuration
public class PokemonModule {

    private static final String TEAM_IMAGE = "/tournament-players/*/team-image";
    private static final String LOCKE_DEATHS = "/lockes/*/deaths";

    /**
     * Declaration of the module.
     *
     * @return the game module
     */
    @Bean
    GameModule pokemonGameModule() {
        return new GameModule("pokemon", "/api/v1/pokemon", List.of(
                new PlayerWrite(HttpMethod.PUT, TEAM_IMAGE),
                new PlayerWrite(HttpMethod.DELETE, TEAM_IMAGE),
                new PlayerWrite(HttpMethod.POST, LOCKE_DEATHS),
                new PlayerWrite(HttpMethod.DELETE, LOCKE_DEATHS + "/*")
        ));
    }
}
