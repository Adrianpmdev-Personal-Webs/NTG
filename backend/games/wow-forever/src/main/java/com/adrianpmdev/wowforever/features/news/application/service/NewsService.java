package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.adrianpmdev.wowforever.features.news.api.dto.response.NewsResponses.NewsFeed;
import com.adrianpmdev.wowforever.features.news.api.dto.response.NewsResponses.NewsItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Serves the launcher news, read from the RSS feed that config.json names. The feed is kept as an on-demand copy of
 * {@code refreshMinutes} (see {@link OnDemandCopy}): read only when someone asks for the news, and the last good news
 * served while the source fails.
 *
 * @author adrianpmdev
 */
@Service
public class NewsService {

    private final GameDataService gameDataService;
    private final NewsFeedClient feedClient;
    private final OnDemandCopy copy;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        this.copy = new OnDemandCopy("The news feed", clock);
    }

    /**
     * The latest news with the credit of their source, as JSON.
     *
     * @return serialized news
     */
    public String getNews() {
        GameDataConfig.News settings = gameDataService.getNewsSettings();
        String json = copy.get(Duration.ofMinutes(settings.refreshMinutes()), () -> read(settings));

        if (json == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The news are not available right now");
        }

        return json;
    }

    private String read(GameDataConfig.News settings) throws IOException {
        byte[] feed = feedClient.fetch(settings.feedUrl(), NewsFeedClient.RSS);
        List<NewsItem> items = NewsFeedParser.parse(feed, settings.category(), settings.limit());

        return objectMapper.writeValueAsString(new NewsFeed(settings.source(), settings.sourceUrl(), items));
    }
}
