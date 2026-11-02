package com.adrianpmdev.lol.features.riot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Calls the Riot API for accounts of EUW. Answers are read as they arrive and only the fields of {@link RiotModels} are
 * kept, so a game of about 80 KB leaves almost nothing in memory.
 *
 * @author adrianpmdev
 */
@Component
public class RiotClient {

    private static final String REGION = "https://europe.api.riotgames.com";
    private static final String PLATFORM = "https://euw1.api.riotgames.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_ATTEMPTS = 4;
    private static final long DEFAULT_RETRY_SECONDS = 10;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int NOT_FOUND = 404;
    private static final int SERVER_ERROR = 500;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final RiotRateLimiter rateLimiter;
    private final String apiKey;

    /**
     * Builds the client.
     *
     * @param rateLimiter limits of the key
     * @param apiKey      Riot API key, which never leaves the backend
     */
    public RiotClient(RiotRateLimiter rateLimiter, @Value("${riot.api.key:}") String apiKey) {
        this.rateLimiter = rateLimiter;
        this.apiKey = apiKey;
    }

    /**
     * Finds an account by its Riot ID.
     *
     * @param gameName name of the Riot ID
     * @param tagLine  tag of the Riot ID
     * @return the account, or empty when it does not exist
     */
    public Optional<RiotModels.Account> account(String gameName, String tagLine) {
        String path = "/riot/account/v1/accounts/by-riot-id/" + encode(gameName) + "/" + encode(tagLine);

        return Optional.ofNullable(get(REGION + path, objectMapper.constructType(RiotModels.Account.class), true));
    }

    /**
     * Ids of the games of a player in a queue, newest first.
     *
     * @param puuid       player
     * @param queueId     queue
     * @param startTime   first moment asked for, in epoch seconds
     * @param start       index of the first one
     * @param count       how many, at most 100
     * @param interactive whether a player is waiting for it
     * @return match ids
     */
    public List<String> matchIds(String puuid, int queueId, long startTime, int start, int count, boolean interactive) {
        String url = REGION + "/lol/match/v5/matches/by-puuid/" + encode(puuid) + "/ids?queue=" + queueId
                + "&startTime=" + startTime + "&start=" + start + "&count=" + count;
        List<String> ids = get(url, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class),
                interactive);

        return ids == null ? List.of() : ids;
    }

    /**
     * One game.
     *
     * @param matchId     match id
     * @param interactive whether a player is waiting for it
     * @return the game, or empty when Riot no longer has it
     */
    public Optional<RiotModels.Match> match(String matchId, boolean interactive) {
        String url = REGION + "/lol/match/v5/matches/" + encode(matchId);

        return Optional.ofNullable(get(url, objectMapper.constructType(RiotModels.Match.class), interactive));
    }

    /**
     * The official rank of a player in each queue.
     *
     * @param puuid       player
     * @param interactive whether a player is waiting for it
     * @return ranks
     */
    public List<RiotModels.Rank> ranks(String puuid, boolean interactive) {
        String url = PLATFORM + "/lol/league/v4/entries/by-puuid/" + encode(puuid);
        List<RiotModels.Rank> ranks = get(url,
                objectMapper.getTypeFactory().constructCollectionType(List.class, RiotModels.Rank.class), interactive);

        return ranks == null ? List.of() : ranks;
    }

    private <T> T get(String url, JavaType type, boolean interactive) {
        if (apiKey.isBlank()) {
            throw new RiotApiException(0, "Falta la clave de la API de Riot en el servidor");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("X-Riot-Token", apiKey)
                .GET()
                .build();
        RiotApiException failure = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                rateLimiter.acquire(interactive);

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                try (InputStream body = response.body()) {
                    int status = response.statusCode();

                    if (status == 200) {
                        return objectMapper.readValue(body, type);
                    }

                    if (status == NOT_FOUND) {
                        return null;
                    }

                    failure = new RiotApiException(status, "Riot ha respondido " + status);

                    if (status == TOO_MANY_REQUESTS) {
                        rateLimiter.pause(response.headers().firstValueAsLong("Retry-After")
                                .orElse(DEFAULT_RETRY_SECONDS));
                    } else if (status < SERVER_ERROR) {
                        throw failure;
                    } else {
                        Thread.sleep(Duration.ofSeconds(attempt + 1L));
                    }
                }
            } catch (IOException exception) {
                failure = new RiotApiException(0, "No se ha podido conectar con Riot");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RiotApiException(0, "La aplicación se está deteniendo");
            }
        }

        throw failure;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
