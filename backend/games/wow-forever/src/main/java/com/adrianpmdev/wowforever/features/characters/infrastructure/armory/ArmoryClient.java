package com.adrianpmdev.wowforever.features.characters.infrastructure.armory;

import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;
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
import java.util.Locale;
import java.util.Optional;

/**
 * Reads the public pages of the official armory with the JDK HTTP client. The page of a character is at its name in
 * lower case with hyphens; when that page does not answer, the search tells whether the character is not there (an
 * unknown character answers an error, just like an armory that is down) or lives at another address.
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

    /** An answer other than 200. */
    private static final class NotAnswered extends IOException {

        NotAnswered(int status) {
            super("The armory answered " + status);
        }
    }

    /**
     * The address of a character in the armory, after {@code /character/}.
     *
     * @param name full name
     * @return the name in lower case, with hyphens for spaces
     */
    public static String slugOf(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    /**
     * Reads a character.
     *
     * @param name full name
     * @return the character, or empty when the armory does not have it
     * @throws IOException when the armory cannot be read
     */
    public Optional<ArmorySheet> character(String name) throws IOException {
        String slug = slugOf(name);

        try {
            return Optional.of(page(slug));
        } catch (NotAnswered notAnswered) {
            Optional<ArmoryMatch> match = ArmoryPage.match(
                    fetch(baseUrl() + "?q=" + URLEncoder.encode(name, StandardCharsets.UTF_8)), name);

            if (match.isEmpty()) {
                return Optional.empty();
            }

            if (match.get().slug().equals(slug)) {
                throw notAnswered;
            }

            return Optional.of(page(match.get().slug()));
        }
    }

    private ArmorySheet page(String slug) throws IOException {
        return ArmoryPage.sheet(fetch(baseUrl() + "/character/"
                + URLEncoder.encode(slug, StandardCharsets.UTF_8).replace("+", "%20")));
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
                throw new NotAnswered(response.statusCode());
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
