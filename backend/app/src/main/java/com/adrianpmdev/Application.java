package com.adrianpmdev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main Spring Boot application class.
 * It lives in the root package on purpose: components, entities and repositories of core and of every game module
 * on the classpath are found without listing them, so adding a game only takes adding its module to this one.
 *
 * @author adrianpmdev
 */
@ConfigurationPropertiesScan
@SpringBootApplication
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
