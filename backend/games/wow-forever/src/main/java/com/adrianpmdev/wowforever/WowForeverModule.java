package com.adrianpmdev.wowforever;

import com.adrianpmdev.platform.module.GameModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the WoW Forever module to the platform: players with the tag read, admins write.
 *
 * @author adrianpmdev
 */
@Configuration
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
