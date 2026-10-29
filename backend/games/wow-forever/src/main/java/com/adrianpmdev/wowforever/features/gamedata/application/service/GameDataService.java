package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.Feature;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.GameClass;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.GameIndex;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Serves the game data of WoW Forever. The data file is built from the game client by GameDataSyncService, which
 * publishes every new one here; {@code wow-forever/config.json} holds the texts and rules the data does not carry.
 * Each screen gets one response with only what it shows, already turned into the API models by
 * {@link GameDataMapper}, so web clients only display it.
 * <p>
 * Responses are serialized once per published file and kept with it, so a new file replaces the data and its
 * responses at once. Until the first file is published the game data endpoints answer 503; the news, which only need
 * the configuration, keep working.
 *
 * @author adrianpmdev
 */
@Service
public class GameDataService {

    private static final String CONFIG_FILE = "wow-forever/config.json";

    /** Sections every screen depends on. A file without one of them is refused instead of serving half the data. */
    private static final List<String> REQUIRED_SECTIONS = List.of(
            "talents", "spellbooks", "spell_desc", "racials", "class_racials", "class_abilities", "legacy");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private volatile GameDataConfig config;
    private volatile GameData data;

    /**
     * A published file: the file as read, its classes by slug (in file order), the mapper built on the configuration
     * and the responses already serialized from it (only keys of classes that exist are added, so it stays small).
     */
    private record GameData(JsonNode root, Map<String, String> classNamesBySlug, GameDataMapper mapper,
                            Map<String, String> responses) {
    }

    /* ─── Publishing ─── */

    /**
     * Checks a data file and, when it is complete, serves it from now on instead of the previous one.
     *
     * @param root the data file
     * @throws IllegalArgumentException when the file lacks a section or a class has no talents, spellbook or races
     */
    public void publish(JsonNode root) {
        validate(root);
        Map<String, String> classNamesBySlug = new LinkedHashMap<>();

        for (Map.Entry<String, JsonNode> entry : root.path("talents").properties()) {
            classNamesBySlug.put(GameDataMapper.slugify(entry.getKey()), entry.getKey());
        }

        data = new GameData(root, Collections.unmodifiableMap(classNamesBySlug), new GameDataMapper(getConfig()),
                new ConcurrentHashMap<>());
    }

    /**
     * Checks that a data file has everything the screens need: every section, at least one class with talent
     * trees and a spellbook, both factions in the races and at least one Legacy tree.
     *
     * @param root the data file
     * @throws IllegalArgumentException naming what is missing
     */
    public static void validate(JsonNode root) {
        for (String section : REQUIRED_SECTIONS) {
            if (!root.path(section).isObject()) {
                throw new IllegalArgumentException("The game data has no \"" + section + "\" section");
            }
        }

        if (root.path("talents").isEmpty()) {
            throw new IllegalArgumentException("The game data has no classes");
        }

        root.path("talents").properties().forEach(entry -> {
            if (entry.getValue().path("trees").isEmpty()
                    || entry.getValue().path("trees").path(0).path("talents").isEmpty()) {
                throw new IllegalArgumentException("The class " + entry.getKey() + " has no talents");
            }

            if (root.path("spellbooks").path(entry.getKey()).path("tabs").isEmpty()) {
                throw new IllegalArgumentException("The class " + entry.getKey() + " has no spellbook");
            }
        });

        for (String faction : List.of("Alliance", "Horde")) {
            if (root.path("racials").path(faction).isEmpty()) {
                throw new IllegalArgumentException("The game data has no " + faction + " races");
            }
        }

        if (root.path("legacy").path("trees").isEmpty()) {
            throw new IllegalArgumentException("The game data has no Legacy trees");
        }
    }

    /** The game build of the file being served, or empty while none is. */
    public Optional<String> currentBuild() {
        GameData current = data;

        return current == null ? Optional.empty() : Optional.ofNullable(current.root().path("build").asText(null));
    }

    /**
     * The texts and rules of the area, read once from config.json.
     *
     * @return the configuration
     */
    public GameDataConfig getConfig() {
        GameDataConfig current = config;

        if (current == null) {
            synchronized (this) {
                current = config;

                if (current == null) {
                    current = readConfig();
                    config = current;
                }
            }
        }

        return current;
    }

    /* ─── Responses ─── */

    /**
     * Credit (with the game build of the first class whose talents name one), classes with their icons, tools of the
     * home, faction names and talent point levels.
     */
    public String getIndex() {
        return cached("index", gameData -> {
            GameDataConfig gameConfig = getConfig();
            List<GameClass> classes = new ArrayList<>();

            gameData.classNamesBySlug().forEach((slug, name) -> classes.add(gameClass(gameData, slug, name)));

            String[] talentSources = gameData.classNamesBySlug().values().stream()
                    .map(name -> gameData.root().path("talents").path(name).path("source").asText(null))
                    .toArray(String[]::new);

            List<Feature> features = gameConfig.features().stream()
                    .map(feature -> new Feature(feature.id(), feature.label(), feature.description(), feature.available()))
                    .toList();
            List<Integer> pointLevels = IntStream
                    .rangeClosed(gameConfig.talents().firstPointLevel(), gameConfig.talents().lastPointLevel())
                    .boxed()
                    .toList();

            return new GameIndex(gameData.mapper().credit(gameData.root(), talentSources), classes, features,
                    gameConfig.factions(), pointLevels);
        });
    }

    /**
     * Where the launcher news come from, as config.json says. It does not need the game data.
     *
     * @return news settings
     */
    public GameDataConfig.News getNewsSettings() {
        GameDataConfig.News news = getConfig().news();

        if (news == null || news.feedUrl() == null || news.limit() <= 0 || news.refreshMinutes() <= 0) {
            throw unavailable(CONFIG_FILE + " has no valid \"news\" section", null);
        }

        return news;
    }

    /**
     * Where the blue posts of the launcher come from, as config.json says. It does not need the game data.
     *
     * @return blue post settings
     */
    public GameDataConfig.BluePosts getBluePostsSettings() {
        GameDataConfig.BluePosts bluePosts = getConfig().bluePosts();

        if (bluePosts == null || bluePosts.trackerUrl() == null || bluePosts.forumUrl() == null
                || bluePosts.categories() == null || bluePosts.limit() <= 0 || bluePosts.maxPages() <= 0
                || bluePosts.refreshMinutes() <= 0) {
            throw unavailable(CONFIG_FILE + " has no valid \"bluePosts\" section", null);
        }

        return bluePosts;
    }

    /** Talent trees of one class. */
    public String getTalents(String classSlug) {
        GameData gameData = data();
        String className = classNameOf(gameData, classSlug);

        return cached("talents/" + classSlug, current -> current.mapper().classTalents(current.root(),
                gameClass(current, classSlug, className), current.root().path("talents").path(className)));
    }

    /** Spellbook of one class, with the tooltip of every spell rank and the class highlights. */
    public String getSpellbook(String classSlug) {
        GameData gameData = data();
        String className = classNameOf(gameData, classSlug);

        return cached("spellbook/" + classSlug, current -> current.mapper().classSpellbook(current.root(),
                gameClass(current, classSlug, className), className));
    }

    /** Every race of both factions with its racial abilities, plus the abilities of some classes that change with it. */
    public String getRacials() {
        return cached("racials", current -> current.mapper().racials(current.root()));
    }

    /** Legacy System: perk trees and challenges. */
    public String getLegacy() {
        return cached("legacy", current -> current.mapper().legacy(current.root(), current.root().path("legacy")));
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

    /** A response of the file being served, built and serialized the first time it is asked for. */
    private String cached(String key, Function<GameData, Object> builder) {
        GameData gameData = data();

        return gameData.responses().computeIfAbsent(key, ignored -> serialize(builder.apply(gameData)));
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

    /* ─── Data and configuration ─── */

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
            throw unavailable("it is being read from the game client", null);
        }

        return current;
    }

    private GameDataConfig readConfig() {
        try (InputStream input = new ClassPathResource(CONFIG_FILE).getInputStream()) {
            return objectMapper.readValue(input, GameDataConfig.class);
        } catch (IOException exception) {
            throw unavailable(CONFIG_FILE + " is missing or is not valid", exception);
        }
    }

    private static ResponseStatusException unavailable(String reason, Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The game data is not available: " + reason,
                cause);
    }
}
