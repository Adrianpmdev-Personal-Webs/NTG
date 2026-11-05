package com.adrianpmdev.lol;

import com.adrianpmdev.platform.module.GameModule;
import com.adrianpmdev.platform.module.GameModule.PlayerWrite;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * Declares the League of Legends module to the platform. Players with the tag read everything; the only writes open to
 * them are linking their own account and asking for the games of a player to be updated. Scheduling is on for the
 * update at dawn (LolDailyUpdate).
 *
 * @author adrianpmdev
 */
@Configuration
@EnableScheduling
public class LolModule {

    /** Key of the tag that grants access to the game. */
    public static final String TAG = "lol";

    /**
     * Declaration of the module.
     *
     * @return the game module
     */
    @Bean
    GameModule lolGameModule() {
        return new GameModule(TAG, "/api/v1/lol", List.of(
                new PlayerWrite(HttpMethod.PUT, "/accounts/me"),
                new PlayerWrite(HttpMethod.DELETE, "/accounts/me"),
                new PlayerWrite(HttpMethod.POST, "/players/*/refresh")
        ));
    }
}
