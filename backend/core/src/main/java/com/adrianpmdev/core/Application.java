package com.adrianpmdev.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application class.
 * Configures component, repository and entity scanning.
 *
 * @author adrianpmdev
 */
@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {
        "com.adrianpmdev.core",
        "com.adrianpmdev.pokemon"
})
@EnableJpaRepositories(basePackages = {
    "com.adrianpmdev.core.features.users.infrastructure.persistence.repository",
    "com.adrianpmdev.core.features.tags.infrastructure.persistence.repository",
    "com.adrianpmdev.pokemon.features.games.infrastructure.persistence.repository",
    "com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository",
    "com.adrianpmdev.pokemon.features.rankings.infrastructure.persistence.repository",
    "com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.repository",
    "com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository"
})
@EntityScan(basePackages = {
    "com.adrianpmdev.core.features.users.infrastructure.persistence.entity",
    "com.adrianpmdev.core.features.tags.infrastructure.persistence.entity",
    "com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity",
    "com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity",
    "com.adrianpmdev.pokemon.features.rankings.infrastructure.persistence.entity",
    "com.adrianpmdev.pokemon.features.rewards.infrastructure.persistence.entity",
    "com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity"
})
public class Application {

    /**
     * Starts the Spring Boot application.
     *
     * @param args application startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}