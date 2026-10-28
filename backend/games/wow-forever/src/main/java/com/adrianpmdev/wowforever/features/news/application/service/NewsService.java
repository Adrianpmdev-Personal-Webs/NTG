package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.adrianpmdev.wowforever.features.news.api.dto.response.NewsResponses.NewsFeed;
import com.adrianpmdev.wowforever.features.news.api.dto.response.NewsResponses.NewsItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Serves the launcher news, read from the RSS feed that config.json names. The feed is only read when someone asks
 * for the news and the copy in memory is older than {@code refreshMinutes}, so the source is read at most once per
 * period however many players open the launcher, and never while nobody does. Requests that arrive while it is being
 * read wait for that same read. When the source fails, the last good news keep being served and the source is not
 * asked again until the period ends.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
public class NewsService {

    private final GameDataService gameDataService;
    private final NewsFeedClient feedClient;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile Snapshot snapshot;

    /** The last read: the news as JSON (null while no read has worked) and when the source was asked. */
    private record Snapshot(String json, Instant readAt) {
    }

    /**
     * Builds the service on the system clock.
     *
     * @param gameDataService source of the news settings
     * @param feedClient      downloads the feed
     */
    @Autowired
    public NewsService(GameDataService gameDataService, NewsFeedClient feedClient) {
        this(gameDataService, feedClient, Clock.systemUTC());
    }

    NewsService(GameDataService gameDataService, NewsFeedClient feedClient, Clock clock) {
        this.gameDataService = gameDataService;
        this.feedClient = feedClient;
        this.clock = clock;
    }

    /**
     * The latest news with the credit of their source, as JSON.
     *
     * @return serialized news
     */
    public String getNews() {
        GameDataConfig.News settings = gameDataService.getNewsSettings();
        Duration refresh = Duration.ofMinutes(settings.refreshMinutes());
        Snapshot current = snapshot;

        if (current == null || isStale(current, refresh)) {
            synchronized (this) {
                current = snapshot;

                if (current == null || isStale(current, refresh)) {
                    current = read(settings, current);
                    snapshot = current;
                }
            }
        }

        if (current.json() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The news are not available right now");
        }

        return current.json();
    }

    private boolean isStale(Snapshot current, Duration refresh) {
        return !current.readAt().plus(refresh).isAfter(clock.instant());
    }

    private Snapshot read(GameDataConfig.News settings, Snapshot previous) {
        Instant now = clock.instant();

        try {
            byte[] feed = feedClient.fetch(settings.feedUrl());
            List<NewsItem> items = NewsFeedParser.parse(feed, settings.category(), settings.limit());
            String json = objectMapper.writeValueAsString(new NewsFeed(settings.source(), settings.sourceUrl(), items));

            return new Snapshot(json, now);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The news could not be written",
                    exception);
        } catch (IOException | RuntimeException exception) {
            log.warn("News feed {} could not be read: {}", settings.feedUrl(), exception.getMessage());

            return new Snapshot(previous == null ? null : previous.json(), now);
        }
    }
}
