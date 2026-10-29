package com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * Reads the game client data that wago.tools publishes: the builds of each product and, for one build, any client
 * table as CSV, in English. Nothing is cached here; the caller decides how often to ask.
 *
 * @author adrianpmdev
 */
@Component
public class WagoClient {

    private static final String BASE_URL = "https://wago.tools";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration BUILDS_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration TABLE_TIMEOUT = Duration.ofSeconds(90);
    private static final String USER_AGENT = "NTG-System/1.0 (+https://github.com/adrianpmdev)";
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+){3}");

    /** Versions by number, part by part: 1.60.1.70658 before 1.60.1.70685. */
    private static final Comparator<String> BY_VERSION = Comparator.comparing(
            (String version) -> Arrays.stream(version.split("\\.")).mapToLong(Long::parseLong).toArray(),
            Arrays::compare);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * The newest build of a product.
     *
     * @param product product code, for example {@code wowf}
     * @return its newest version, or empty when wago.tools lists none
     * @throws IOException when wago.tools cannot be reached or answers an error
     */
    public Optional<String> latestBuild(String product) throws IOException {
        try (InputStream body = get("/api/builds", BUILDS_TIMEOUT)) {
            return latestBuild(objectMapper.readTree(body), product);
        }
    }

    static Optional<String> latestBuild(JsonNode builds, String product) {
        return StreamSupport.stream(builds.path(product).spliterator(), false)
                .map(build -> build.path("version").asText(""))
                .filter(version -> VERSION.matcher(version).matches())
                .max(BY_VERSION);
    }

    /**
     * The tables of one build.
     *
     * @param build version, for example {@code 1.60.1.70685}
     * @return a source that downloads each table when it is read
     */
    public Db2Source tablesOf(String build) {
        if (!VERSION.matcher(build).matches()) {
            throw new IllegalArgumentException("Not a build version: " + build);
        }

        return (name, columns, keep) -> {
            String path = "/db2/" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "/csv?build=" + build;

            try (Reader csv = new InputStreamReader(get(path, TABLE_TIMEOUT), StandardCharsets.UTF_8)) {
                return Db2Table.read(name, csv, columns, keep);
            }
        };
    }

    private InputStream get(String path, Duration timeout) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                response.body().close();
                throw new IOException("wago.tools answered " + response.statusCode() + " for " + path);
            }

            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("The request to wago.tools was interrupted", exception);
        }
    }
}
