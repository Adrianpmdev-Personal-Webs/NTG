package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.adrianpmdev.wowforever.features.news.api.dto.response.BluePostResponses.BluePost;
import com.adrianpmdev.wowforever.features.news.api.dto.response.BluePostResponses.BluePostFeed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serves the blue posts of the launcher: the latest posts of Blizzard employees about WoW Forever, read from the
 * Blizzard tracker of the official forums. The tracker mixes every version of the game, so it is read page by page
 * (at most {@code maxPages}) until {@code limit} posts about WoW Forever are found. Blizzard sometimes opens the same
 * announcement in two forums: only the newest topic with a title and an author is kept (answers inside a topic are
 * always kept, each is a different post). The result is kept as an on-demand copy of {@code refreshMinutes} (see {@link OnDemandCopy}): read only when someone asks for the posts, and
 * the last good posts served while the forums fail.
 *
 * @author adrianpmdev
 */
@Service
public class BluePostsService {

    private final GameDataService gameDataService;
    private final NewsFeedClient feedClient;
    private final OnDemandCopy copy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Builds the service on the system clock.
     *
     * @param gameDataService source of the blue post settings
     * @param feedClient      downloads the tracker pages
     */
    @Autowired
    public BluePostsService(GameDataService gameDataService, NewsFeedClient feedClient) {
        this(gameDataService, feedClient, Clock.systemUTC());
    }

    BluePostsService(GameDataService gameDataService, NewsFeedClient feedClient, Clock clock) {
        this.gameDataService = gameDataService;
        this.feedClient = feedClient;
        this.copy = new OnDemandCopy("The blue tracker", clock);
    }

    /**
     * The latest blue posts with the credit of their source, as JSON.
     *
     * @return serialized blue posts
     */
    public String getBluePosts() {
        GameDataConfig.BluePosts settings = gameDataService.getBluePostsSettings();
        String json = copy.get(Duration.ofMinutes(settings.refreshMinutes()), () -> read(settings));

        if (json == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The blue posts are not available right now");
        }

        return json;
    }

    private String read(GameDataConfig.BluePosts settings) throws IOException {
        List<BluePost> posts = new ArrayList<>();
        Set<String> topics = new HashSet<>();
        Long before = null;

        for (int page = 0; page < settings.maxPages() && posts.size() < settings.limit(); page++) {
            byte[] json = feedClient.fetch(pageUrl(settings.trackerUrl(), before), NewsFeedClient.JSON);
            BlueTrackerParser.Page read = BlueTrackerParser.parse(json, settings);

            for (BluePost post : read.posts()) {
                if (posts.size() < settings.limit() && isFirstCopy(post, topics)) {
                    posts.add(post);
                }
            }

            if (read.oldestId() == null) {
                break;
            }

            before = read.oldestId();
        }

        return objectMapper.writeValueAsString(new BluePostFeed(settings.source(), settings.sourceUrl(), posts));
    }

    private static boolean isFirstCopy(BluePost post, Set<String> topics) {
        return post.reply() || topics.add(post.author() + "\n" + post.title());
    }

    private static String pageUrl(String trackerUrl, Long before) {
        return before == null ? trackerUrl : trackerUrl + "?before_post_id=" + before;
    }
}
