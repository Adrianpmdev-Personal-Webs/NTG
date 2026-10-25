package com.adrianpmdev.wowforever.features.talents.application.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves the talent trees of the game. The data is the talents.json file published by wow-forever.gg, kept in the
 * module resources and sent as it is: the frontend knows its format, so there is nothing to map or to store.
 *
 * @author adrianpmdev
 */
@Service
public class TalentService {

    private static final String TALENTS_FILE = "wow-forever/talents.json";

    private volatile String cachedJson;

    /** The content of talents.json, read once. A missing file only breaks this endpoint, not the whole backend. */
    public String getTalentsJson() {
        String json = cachedJson;

        if (json == null) {
            json = readTalentsFile();
            cachedJson = json;
        }

        return json;
    }

    private String readTalentsFile() {
        try (InputStream input = new ClassPathResource(TALENTS_FILE).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The talent data is not available: " + TALENTS_FILE + " is missing", exception);
        }
    }
}
