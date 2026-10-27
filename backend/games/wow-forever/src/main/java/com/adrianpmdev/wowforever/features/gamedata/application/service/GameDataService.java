package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.Feature;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.GameClass;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.GameIndex;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.NewsItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Serves the game data of WoW Forever. Everything comes from two files: {@code wow-forever/data.json}, the export
 * published by talentsforever.com (or a copy of it kept by hand), and {@code wow-forever/config.json}, with the texts
 * and rules the export does not carry. Both are read once; each screen gets one response with only what it shows,
 * already turned into the API models by {@link GameDataMapper}, so web clients only display it.
 * <p>
 * Responses are serialized once and kept in memory: the data only changes with a new deploy. A missing or broken file
 * only breaks these endpoints, not the whole backend, and it is read again on the next request.
 *
 * @author adrianpmdev
 */
@Service
public class GameDataService {

    private static final String DATA_FILE = "wow-forever/data.json";
    private static final String CONFIG_FILE = "wow-forever/config.json";

    /** Sections every screen depends on. If one is missing the file is rejected instead of serving half the data. */
    private static final List<String> REQUIRED_SECTIONS = List.of(
            "talents", "spellbooks", "spell_desc", "racials", "class_racials", "class_abilities", "legacy");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Serialized responses, built on first use. Keys are only added for classes that exist, so the map stays small. */
    private final Map<String, String> responses = new ConcurrentHashMap<>();

    private volatile GameData data;

    /** The files as read, the classes the data holds by slug (in file order) and the mapper built on the config. */
    private record GameData(JsonNode root, Map<String, String> classNamesBySlug, GameDataConfig config,
                            GameDataMapper mapper) {
    }

    /** Credit, classes with their icons, tools and news of the home, faction names and talent point levels. */
    public String getIndex() {
        return cached("index", () -> {
            GameData gameData = data();
            GameDataConfig config = gameData.config();
            List<GameClass> classes = new ArrayList<>();

            gameData.classNamesBySlug().forEach((slug, name) -> classes.add(gameClass(gameData, slug, name)));

            List<Feature> features = config.features().stream()
                    .map(feature -> new Feature(feature.id(), feature.label(), feature.description(), feature.available()))
                    .toList();
            List<NewsItem> news = config.news().stream()
                    .map(item -> new NewsItem(item.id(), item.date(), item.title(), item.image()))
                    .toList();
            List<Integer> pointLevels = IntStream
                    .rangeClosed(config.talents().firstPointLevel(), config.talents().lastPointLevel())
                    .boxed()
                    .toList();

            return new GameIndex(gameData.mapper().credit(gameData.root()), classes, features, news, config.factions(),
                    pointLevels);
        });
    }

    /** Talent trees of one class. */
    public String getTalents(String classSlug) {
        GameData gameData = data();
        String className = classNameOf(gameData, classSlug);

        return cached("talents/" + classSlug, () -> gameData.mapper().classTalents(gameData.root(),
                gameClass(gameData, classSlug, className), gameData.root().path("talents").path(className)));
    }

    /** Spellbook of one class, with the tooltip of every spell rank and the class highlights. */
    public String getSpellbook(String classSlug) {
        GameData gameData = data();
        String className = classNameOf(gameData, classSlug);

        return cached("spellbook/" + classSlug, () -> gameData.mapper().classSpellbook(gameData.root(),
                gameClass(gameData, classSlug, className), className));
    }

    /** Every race of both factions with its racial abilities, plus the abilities of some classes that change with it. */
    public String getRacials() {
        return cached("racials", () -> data().mapper().racials(data().root()));
    }

    /** Legacy System: perk trees and challenges. */
    public String getLegacy() {
        return cached("legacy", () -> data().mapper().legacy(data().root(), data().root().path("legacy")));
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
                treeSlugs.add(GameDataMapper.slugify(tree.path("name").asText("")));
            }

            trees.put(slug, List.copyOf(treeSlugs));
        });

        return trees;
    }

    /* ─── Responses ─── */

    private String cached(String key, Supplier<Object> builder) {
        String json = responses.get(key);

        if (json == null) {
            json = serialize(builder.get());
            responses.put(key, json);
        }

        return json;
    }

    private static GameClass gameClass(GameData gameData, String slug, String name) {
        return gameData.mapper().gameClass(slug, name, gameData.root().path("talents").path(name).path("icon").asText(""));
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The game data could not be written",
                    exception);
        }
    }

    /* ─── Files ─── */

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
        GameDataConfig config;

        try (InputStream input = new ClassPathResource(DATA_FILE).getInputStream()) {
            root = objectMapper.readTree(input);
        } catch (IOException exception) {
            throw unavailable(DATA_FILE + " is missing or is not valid JSON", exception);
        }

        try (InputStream input = new ClassPathResource(CONFIG_FILE).getInputStream()) {
            config = objectMapper.readValue(input, GameDataConfig.class);
        } catch (IOException exception) {
            throw unavailable(CONFIG_FILE + " is missing or is not valid", exception);
        }

        for (String section : REQUIRED_SECTIONS) {
            if (!root.path(section).isObject()) {
                throw unavailable(DATA_FILE + " has no \"" + section + "\" section", null);
            }
        }

        Map<String, String> classNamesBySlug = new LinkedHashMap<>();

        for (Map.Entry<String, JsonNode> entry : root.path("talents").properties()) {
            classNamesBySlug.put(GameDataMapper.slugify(entry.getKey()), entry.getKey());
        }

        if (classNamesBySlug.isEmpty()) {
            throw unavailable(DATA_FILE + " has no classes in \"talents\"", null);
        }

        return new GameData(root, Collections.unmodifiableMap(classNamesBySlug), config, new GameDataMapper(config));
    }

    private static ResponseStatusException unavailable(String reason, Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The game data is not available: " + reason,
                cause);
    }
}
