package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Serves the game data of WoW Forever. Everything comes from one file, {@code wow-forever/data.json}, the export
 * published by talentsforever.com (or a copy of it kept by hand): it is read once and cut into one response per
 * screen, so a screen only downloads what it shows.
 * <p>
 * The sections are sent as they are in the file: the frontend is the only place that knows their format. The service
 * only adds a {@code meta} block (licence and credit) and the class each response belongs to. A missing or broken
 * file only breaks these endpoints, not the whole backend, and it is read again on the next request.
 *
 * @author adrianpmdev
 */
@Service
public class GameDataService {

    private static final String DATA_FILE = "wow-forever/data.json";

    /** Sections every screen depends on. If one is missing the file is rejected instead of serving half the data. */
    private static final List<String> REQUIRED_SECTIONS = List.of(
            "talents", "spellbooks", "spell_desc", "racials", "class_racials", "class_abilities", "legacy");

    private static final String SPELL_KEY_SEPARATOR = "|";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Serialized responses, built on first use. Keys are only added for classes that exist, so the map stays small. */
    private final Map<String, String> responses = new ConcurrentHashMap<>();

    private volatile GameData data;

    /** The file as read, plus the classes it holds by slug, in file order. */
    private record GameData(JsonNode root, Map<String, String> classNamesBySlug) {
    }

    /** Credit and the list of classes, with the icon each one shows in the class pickers. */
    public String getIndex() {
        return cached("index", () -> {
            GameData gameData = data();
            ObjectNode response = response(gameData);
            ArrayNode classes = response.putArray("classes");

            gameData.classNamesBySlug().forEach((slug, name) -> classes.add(classNode(gameData, slug, name)));

            return response;
        });
    }

    /** Talent trees of one class. */
    public String getTalents(String classSlug) {
        GameData gameData = data();
        String className = classNameOf(gameData, classSlug);

        return cached("talents/" + classSlug, () -> {
            ObjectNode response = classResponse(gameData, classSlug, className);
            response.set("talents", section(gameData.root().path("talents").path(className)));

            return response;
        });
    }

    /** Spellbook of one class, with the tooltip text of every spell rank and the class highlights. */
    public String getSpellbook(String classSlug) {
        GameData gameData = data();
        String className = classNameOf(gameData, classSlug);

        return cached("spellbook/" + classSlug, () -> {
            JsonNode root = gameData.root();
            ObjectNode response = classResponse(gameData, classSlug, className);
            response.set("spellbook", section(root.path("spellbooks").path(className)));

            // Spell texts are keyed "<class>|<spell>|<rank>": the class part is dropped, the rest is kept as is.
            String prefix = className + SPELL_KEY_SEPARATOR;
            ObjectNode spells = response.putObject("spells");

            for (Map.Entry<String, JsonNode> entry : root.path("spell_desc").properties()) {
                if (entry.getKey().startsWith(prefix)) {
                    spells.set(entry.getKey().substring(prefix.length()), entry.getValue());
                }
            }

            JsonNode highlights = root.path("class_abilities").path(className);
            response.set("highlights", highlights.isArray() ? highlights : objectMapper.createArrayNode());

            return response;
        });
    }

    /** Racial abilities of every race, by faction, plus the racials that depend on the class. */
    public String getRacials() {
        return cached("racials", () -> {
            GameData gameData = data();
            ObjectNode response = response(gameData);
            response.set("factions", section(gameData.root().path("racials")));
            response.set("classRacials", section(gameData.root().path("class_racials")));

            return response;
        });
    }

    /** Legacy System: perk trees and challenges. */
    public String getLegacy() {
        return cached("legacy", () -> {
            GameData gameData = data();
            ObjectNode response = response(gameData);
            response.set("legacy", section(gameData.root().path("legacy")));

            return response;
        });
    }

    /**
     * Talent trees of every class, as slugs: the class slug and the slugs of its trees, in file order.
     *
     * @return tree slugs by class slug
     */
    public Map<String, List<String>> getTalentTreeSlugs() {
        GameData gameData = data();
        Map<String, List<String>> trees = new LinkedHashMap<>();

        gameData.classNamesBySlug().forEach((slug, name) -> {
            List<String> treeSlugs = new ArrayList<>();

            for (JsonNode tree : gameData.root().path("talents").path(name).path("trees")) {
                treeSlugs.add(slugify(tree.path("name").asText("")));
            }

            trees.put(slug, List.copyOf(treeSlugs));
        });

        return trees;
    }

    /* ─── Responses ─── */

    private String cached(String key, Supplier<JsonNode> builder) {
        String json = responses.get(key);

        if (json == null) {
            json = serialize(builder.get());
            responses.put(key, json);
        }

        return json;
    }

    private ObjectNode response(GameData gameData) {
        JsonNode root = gameData.root();
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode meta = response.putObject("meta");

        meta.put("license", root.path("license").asText(""));
        meta.put("attribution", root.path("attribution").asText(""));
        meta.put("generated", root.path("generated").asText(""));

        return response;
    }

    private ObjectNode classResponse(GameData gameData, String slug, String name) {
        ObjectNode response = response(gameData);
        response.set("class", classNode(gameData, slug, name));

        return response;
    }

    private ObjectNode classNode(GameData gameData, String slug, String name) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("slug", slug);
        node.put("name", name);
        node.put("icon", gameData.root().path("talents").path(name).path("icon").asText(""));

        return node;
    }

    /** A section of the file, or an empty object when the file does not have it. */
    private JsonNode section(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? objectMapper.createObjectNode() : node;
    }

    private String serialize(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The game data could not be written",
                    exception);
        }
    }

    /* ─── File ─── */

    private String classNameOf(GameData gameData, String slug) {
        String name = gameData.classNamesBySlug().get(slug);

        if (name == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown class: " + slug);
        }

        return name;
    }

    private GameData data() {
        GameData current = data;

        if (current == null) {
            synchronized (this) {
                current = data;

                if (current == null) {
                    current = load();
                    data = current;
                }
            }
        }

        return current;
    }

    private GameData load() {
        JsonNode root;

        try (InputStream input = new ClassPathResource(DATA_FILE).getInputStream()) {
            root = objectMapper.readTree(input);
        } catch (IOException exception) {
            throw unavailable(DATA_FILE + " is missing or is not valid JSON", exception);
        }

        for (String section : REQUIRED_SECTIONS) {
            if (!root.path(section).isObject()) {
                throw unavailable(DATA_FILE + " has no \"" + section + "\" section", null);
            }
        }

        Map<String, String> classNamesBySlug = new LinkedHashMap<>();

        for (Map.Entry<String, JsonNode> entry : root.path("talents").properties()) {
            classNamesBySlug.put(slugify(entry.getKey()), entry.getKey());
        }

        if (classNamesBySlug.isEmpty()) {
            throw unavailable(DATA_FILE + " has no classes in \"talents\"", null);
        }

        return new GameData(root, Collections.unmodifiableMap(classNamesBySlug));
    }

    private static ResponseStatusException unavailable(String reason, Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The game data is not available: " + reason,
                cause);
    }

    /** Lower case, words joined by dashes: "Night Elf" becomes "night-elf". The frontend builds slugs the same way. */
    private static String slugify(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }
}
