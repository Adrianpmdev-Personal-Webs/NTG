package com.adrianpmdev.wowforever.features.characters.infrastructure.armory;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Reads the public pages of the official armory with the JDK HTTP client. The search says whether a character exists,
 * because the page of an unknown character answers an error, just like the armory when it is down.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class ArmoryClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_CHARS = 2 * 1024 * 1024;
    private static final String USER_AGENT = "NTG-System/1.0 (+https://github.com/adrianpmdev)";

    private final GameDataService gameDataService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Looks for a character.
     *
     * @param name full name
     * @return the character, or empty when the armory does not have it
     * @throws IOException when the armory cannot be read
     */
    public Optional<ArmoryMatch> search(String name) throws IOException {
        return ArmoryPage.match(fetch(baseUrl() + "?q=" + URLEncoder.encode(name, StandardCharsets.UTF_8)), name);
    }

    /**
     * Reads a character.
     *
     * @param slug its id in the armory
     * @return the character
     * @throws IOException when the armory cannot be read
     */
    public ArmoryCharacter read(String slug) throws IOException {
        String path = URLEncoder.encode(slug, StandardCharsets.UTF_8).replace("+", "%20");

        return ArmoryPage.character(fetch(baseUrl() + "/character/" + path));
    }

    private String baseUrl() {
        return gameDataService.getConfig().armory().dataUrl();
    }

    private String fetch(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new IOException("The armory answered " + response.statusCode());
            }

            if (response.body().length() > MAX_CHARS) {
                throw new IOException("The armory sent too much");
            }

            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("The armory request was interrupted", exception);
        }
    }
}
