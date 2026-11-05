package com.adrianpmdev.wowforever;

import com.adrianpmdev.platform.module.GameModule;
import com.adrianpmdev.platform.module.GameModule.PlayerWrite;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * Declares the WoW Forever module to the platform: players with the tag read, admins write. The only writes open to
 * players are the characters, and the module then accepts only their owner (or an admin). Scheduling is on for the
 * hourly check of new game builds (GameDataSyncService).
 *
 * @author adrianpmdev
 */
@Configuration
@EnableScheduling
public class WowForeverModule {

    /** Key of the tag that grants access to the game. */
    public static final String TAG = "wow-forever";

    private static final String CHARACTERS = "/characters";
    private static final String OWN_BUILDS = "/me/builds/*/*";

    /**
     * Declaration of the module.
     *
     * @return the game module
     */
    @Bean
    GameModule wowForeverGameModule() {
        return new GameModule(TAG, "/api/v1/wow-forever", List.of(
                new PlayerWrite(HttpMethod.POST, CHARACTERS),
                new PlayerWrite(HttpMethod.POST, CHARACTERS + "/refresh"),
                new PlayerWrite(HttpMethod.PUT, CHARACTERS + "/*"),
                new PlayerWrite(HttpMethod.DELETE, CHARACTERS + "/*"),
                new PlayerWrite(HttpMethod.PUT, OWN_BUILDS),
                new PlayerWrite(HttpMethod.DELETE, OWN_BUILDS),
                new PlayerWrite(HttpMethod.PUT, "/me/legacy-challenges")
        ));
    }
}
