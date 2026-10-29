package com.adrianpmdev.wowforever;

import com.adrianpmdev.platform.module.GameModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Declares the WoW Forever module to the platform: players with the tag read, admins write. Scheduling is on for the
 * hourly check of new game builds (GameDataSyncService).
 *
 * @author adrianpmdev
 */
@Configuration
@EnableScheduling
public class WowForeverModule {

    /**
     * Declaration of the module.
     *
     * @return the game module
     */
    @Bean
    GameModule wowForeverGameModule() {
        return GameModule.of("wow-forever", "/api/v1/wow-forever");
    }
}
