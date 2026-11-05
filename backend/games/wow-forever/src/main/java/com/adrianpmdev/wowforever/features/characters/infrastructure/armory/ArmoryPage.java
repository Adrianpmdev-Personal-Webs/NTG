package com.adrianpmdev.wowforever.features.characters.infrastructure.armory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads the pages of the official armory, which carry their data as JSON in a script called model. That format is not
 * documented and can change: anything missing is an error, so a changed page never overwrites good data.
 *
 * @author adrianpmdev
 */
final class ArmoryPage {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MODEL = "<script id=\"model\">model = ";
    private static final String CHARACTER_CATEGORY = "CHARACTER";

    private ArmoryPage() {
    }

    /**
     * Finds a character in a search page. The search also gives similar names, so only the exact one counts.
     *
     * @param html page of the search
     * @param name full name wanted, in any case
     * @return the character, or empty when the armory does not have it
     * @throws IOException when the page is not what was expected
     */
    static Optional<ArmoryMatch> match(String html, String name) throws IOException {
        JsonNode categories = model(html).path("categories");

        if (!categories.isArray()) {
            throw new IOException("The armory search has no categories");
        }

        for (JsonNode category : categories) {
            if (CHARACTER_CATEGORY.equals(category.path("enum").asText())) {
                for (JsonNode result : category.path("results")) {
                    if (fullName(result).equalsIgnoreCase(name) && result.path("slug").isTextual()) {
                        return Optional.of(new ArmoryMatch(result.path("slug").asText(), lastUpdated(result)));
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Reads a character page.
     *
     * @param html page of the character
     * @return the character
     * @throws IOException when the page is not what was expected
     */
    static ArmoryCharacter character(String html) throws IOException {
        JsonNode character = model(html).path("reactMounts").path(0).path("initialState").path("character");
        JsonNode professions = character.path("professions");

        if (!character.path("slug").isTextual() || !character.path("level").isInt()
                || !character.path("class").path("name").isTextual()
                || !character.path("race").path("name").isTextual()) {
            throw new IOException("The armory page has no character");
        }

        List<ArmoryCharacter.Profession> known = new ArrayList<>();
        addProfessions(professions.path("primaries"), known);
        addProfessions(professions.path("secondaries"), known);

        return new ArmoryCharacter(character.path("slug").asText(), fullName(character),
                character.path("class").path("name").asText(), character.path("race").path("name").asText(),
                character.path("level").asInt(), known, lastUpdated(character));
    }

    /** A profession can list several tiers: what it can reach is told by the highest one. */
    private static void addProfessions(JsonNode list, List<ArmoryCharacter.Profession> known) {
        for (JsonNode profession : list) {
            JsonNode highest = null;

            for (JsonNode tier : profession.path("tiers")) {
                if (highest == null || tier.path("maxSkillPoints").asInt() > highest.path("maxSkillPoints").asInt()) {
                    highest = tier;
                }
            }

            if (highest != null) {
                known.add(new ArmoryCharacter.Profession(profession.path("name").asText(),
                        highest.path("skillPoints").asInt(), highest.path("maxSkillPoints").asInt()));
            }
        }
    }

    private static String fullName(JsonNode character) {
        return (character.path("name").asText() + " " + character.path("surname").asText()).trim();
    }

    private static Instant lastUpdated(JsonNode character) {
        JsonNode epoch = character.path("lastUpdatedTimestamp").path("epoch");

        return epoch.isNumber() ? Instant.ofEpochMilli(epoch.asLong()) : null;
    }

    private static JsonNode model(String html) throws IOException {
        int start = html.indexOf(MODEL);

        if (start < 0) {
            throw new IOException("The armory page has no model");
        }

        try {
            return JSON.readTree(html.substring(start + MODEL.length()));
        } catch (JsonProcessingException exception) {
            throw new IOException("The armory model could not be read", exception);
        }
    }
}
