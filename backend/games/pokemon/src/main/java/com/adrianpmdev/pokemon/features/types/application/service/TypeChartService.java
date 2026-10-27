package com.adrianpmdev.pokemon.features.types.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Pokémon types and their damage chart, from {@code pokemon/types.json}: the list of types in display order and, for
 * every attacking type, the multiplier against each defending type that is not 1. The file is read and checked once.
 *
 * @author adrianpmdev
 */
@Service
public class TypeChartService {

    private static final String FILE = "pokemon/types.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String json;

    /**
     * The types and the chart, as JSON: {@code {"types": [...], "chart": {"Fuego": {"Planta": 2, ...}, ...}}}.
     *
     * @return serialized chart
     */
    public String getChart() {
        String current = json;

        if (current == null) {
            synchronized (this) {
                current = json;

                if (current == null) {
                    current = load();
                    json = current;
                }
            }
        }

        return current;
    }

    private String load() {
        try (InputStream input = new ClassPathResource(FILE).getInputStream()) {
            JsonNode root = objectMapper.readTree(input);

            if (!root.path("types").isArray() || !root.path("chart").isObject()) {
                throw unavailable(FILE + " needs a \"types\" list and a \"chart\" object", null);
            }

            return objectMapper.writeValueAsString(root);
        } catch (IOException exception) {
            throw unavailable(FILE + " is missing or is not valid JSON", exception);
        }
    }

    private static ResponseStatusException unavailable(String reason, Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The type chart is not available: " + reason,
                cause);
    }
}
