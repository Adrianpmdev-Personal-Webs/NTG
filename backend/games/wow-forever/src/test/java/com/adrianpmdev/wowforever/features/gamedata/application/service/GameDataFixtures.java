package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * The test data file (test resource {@code wow-forever/game-data.json}): a small file cut from a real export, with
 * one class, a few talents and spells, one race per faction and one Legacy tree.
 *
 * @author adrianpmdev
 */
public final class GameDataFixtures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GameDataFixtures() {
    }

    /** The test data file, read again on every call so a test can change its copy. */
    public static ObjectNode testData() {
        try (InputStream input = new ClassPathResource("wow-forever/game-data.json").getInputStream()) {
            return (ObjectNode) OBJECT_MAPPER.readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("The test data file cannot be read", exception);
        }
    }

    /** A service serving the test data file. */
    public static GameDataService published() {
        GameDataService service = new GameDataService();
        service.publish(testData());

        return service;
    }
}
