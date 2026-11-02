package com.adrianpmdev.lol.features.riot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Names and icons of champions, items and summoner spells, from Data Dragon. It asks once a day, keeps only small maps
 * in memory, and if it cannot read them the screens go on without icons. Browsers load the pictures from Riot, so they
 * cost nothing to this server.
 *
 * @author adrianpmdev
 */
@Slf4j
@Component
public class DataDragon {

    private static final String BASE = "https://ddragon.leagueoflegends.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final Duration KEEP_FOR = Duration.ofHours(24);
    private static final Duration RETRY_AFTER = Duration.ofMinutes(10);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private volatile Catalog catalog = new Catalog("", Map.of(), Map.of());
    private Instant nextReadAt = Instant.EPOCH;

    private record Catalog(String version, Map<Integer, Champion> champions, Map<Integer, String> spells) {
    }

    private record Champion(String name, String image) {
    }

    /**
     * Name of a champion.
     *
     * @param championId champion
     * @param fallback   what to show when it is not known
     * @return its name
     */
    public String championName(int championId, String fallback) {
        Champion champion = current().champions().get(championId);

        return champion == null ? fallback : champion.name();
    }

    /**
     * Icon of a champion.
     *
     * @param championId champion
     * @return its URL, or null when it is not known
     */
    public String championIcon(int championId) {
        Catalog current = current();
        Champion champion = current.champions().get(championId);

        return champion == null ? null : imageBase(current) + "/champion/" + champion.image();
    }

    /**
     * Icon of an item.
     *
     * @param itemId item
     * @return its URL, or null when it is not known
     */
    public String itemIcon(int itemId) {
        Catalog current = current();

        return current.version().isEmpty() || itemId == 0 ? null : imageBase(current) + "/item/" + itemId + ".png";
    }

    /**
     * Icon of a summoner spell.
     *
     * @param spellId summoner spell
     * @return its URL, or null when it is not known
     */
    public String spellIcon(int spellId) {
        Catalog current = current();
        String image = current.spells().get(spellId);

        return image == null ? null : imageBase(current) + "/spell/" + image;
    }

    /**
     * Profile icon of a summoner.
     *
     * @param iconId profile icon, or null when it is not known yet
     * @return its URL, or null when it is not known
     */
    public String profileIcon(Integer iconId) {
        Catalog current = current();

        return current.version().isEmpty() || iconId == null ? null
                : imageBase(current) + "/profileicon/" + iconId + ".png";
    }

    private static String imageBase(Catalog catalog) {
        return BASE + "/cdn/" + catalog.version() + "/img";
    }

    private Catalog current() {
        Instant now = Instant.now();

        synchronized (this) {
            if (now.isAfter(nextReadAt)) {
                try {
                    catalog = read();
                    nextReadAt = now.plus(KEEP_FOR);
                } catch (IOException | RuntimeException exception) {
                    nextReadAt = now.plus(RETRY_AFTER);
                    log.warn("Data Dragon could not be read: {}", exception.getMessage());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return catalog;
    }

    private Catalog read() throws IOException, InterruptedException {
        String version = fetch(BASE + "/api/versions.json").path(0).asText();
        String data = BASE + "/cdn/" + version + "/data/en_US/";
        Map<Integer, Champion> champions = new HashMap<>();
        Map<Integer, String> spells = new HashMap<>();

        for (JsonNode champion : fetch(data + "champion.json").path("data")) {
            champions.put(champion.path("key").asInt(),
                    new Champion(champion.path("name").asText(), champion.path("image").path("full").asText()));
        }

        for (JsonNode spell : fetch(data + "summoner.json").path("data")) {
            spells.put(spell.path("key").asInt(), spell.path("image").path("full").asText());
        }

        return new Catalog(version, Map.copyOf(champions), Map.copyOf(spells));
    }

    private JsonNode fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("Data Dragon answered " + response.statusCode());
            }

            return objectMapper.readTree(body);
        }
    }
}
