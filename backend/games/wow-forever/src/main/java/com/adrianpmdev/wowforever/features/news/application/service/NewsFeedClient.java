package com.adrianpmdev.wowforever.features.news.application.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Downloads the news feed with the JDK HTTP client. It gives up after a few seconds and refuses feeds that are too
 * big, so a slow or broken source never holds the launcher for long.
 *
 * @author adrianpmdev
 */
@Component
public class NewsFeedClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final String USER_AGENT = "NTG-System/1.0 (+https://github.com/adrianpmdev)";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Reads the feed.
     *
     * @param url feed address
     * @return the feed as bytes
     * @throws IOException when the source cannot be reached, answers an error or sends too much
     */
    public byte[] fetch(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/rss+xml, application/xml;q=0.9")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new IOException("The news feed answered " + response.statusCode());
            }

            if (response.body().length > MAX_BYTES) {
                throw new IOException("The news feed is too big");
            }

            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("The news feed request was interrupted", exception);
        }
    }
}
