package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How many tracker pages are read to find the blue posts, and that a read is kept for the period of config.json.
 *
 * @author adrianpmdev
 */
class BluePostsServiceTest {

    private final FakeTrackerClient trackerClient = new FakeTrackerClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsPagesUntilTheTrackerEndsAndKeepsOneCopyOfEachTopic() throws Exception {
        JsonNode posts = objectMapper.readTree(service(20, 5).getBluePosts());

        assertThat(trackerClient.urls).containsExactly("https://forums.test/tracker.json",
                "https://forums.test/tracker.json?before_post_id=101",
                "https://forums.test/tracker.json?before_post_id=96");
        assertThat(posts.path("items")).extracting(item -> item.path("id").asLong())
                .containsExactly(105L, 103L, 102L, 99L);
        assertThat(posts.path("source").asText()).isEqualTo("Blizzard Forums");
    }

    @Test
    void stopsAtTheLimit() throws Exception {
        JsonNode posts = objectMapper.readTree(service(2, 5).getBluePosts());

        assertThat(trackerClient.urls).hasSize(1);
        assertThat(posts.path("items")).hasSize(2);
    }

    @Test
    void stopsAtTheMostPages() throws Exception {
        JsonNode posts = objectMapper.readTree(service(20, 1).getBluePosts());

        assertThat(trackerClient.urls).hasSize(1);
        assertThat(posts.path("items")).hasSize(3);
    }

    @Test
    void readsTheTrackerOncePerPeriod() {
        BluePostsService service = service(2, 5);
        service.getBluePosts();
        service.getBluePosts();

        assertThat(trackerClient.urls).hasSize(1);
    }

    @Test
    void answersUnavailableUntilAReadWorks() {
        trackerClient.failing = true;

        assertThatThrownBy(service(20, 5)::getBluePosts).isInstanceOf(ResponseStatusException.class);
    }

    private BluePostsService service(int limit, int maxPages) {
        GameDataService gameDataService = new GameDataService() {
            @Override
            public GameDataConfig.BluePosts getBluePostsSettings() {
                return BlueTrackerParserTest.settings(limit, maxPages);
            }
        };

        return new BluePostsService(gameDataService, trackerClient,
                Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC));
    }

    private static final class FakeTrackerClient extends NewsFeedClient {

        private final List<String> urls = new ArrayList<>();
        private boolean failing;

        @Override
        public byte[] fetch(String url, String accept) throws IOException {
            urls.add(url);

            if (failing) {
                throw new IOException("offline");
            }

            int page = url.endsWith("before_post_id=101") ? 2 : url.endsWith("before_post_id=96") ? 3 : 1;

            return BlueTrackerParserTest.page(page);
        }
    }
}
