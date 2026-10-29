package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * When the news feed is read again (only after the period of config.json) and what is served when it fails.
 *
 * @author adrianpmdev
 */
class NewsServiceTest {

    private final MovableClock clock = new MovableClock();
    private final FakeFeedClient feedClient = new FakeFeedClient();
    private final NewsService service = new NewsService(new GameDataService(), feedClient, clock);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsTheFeedOncePerPeriod() throws Exception {
        JsonNode news = objectMapper.readTree(service.getNews());
        service.getNews();
        clock.advance(Duration.ofMinutes(4));
        service.getNews();

        assertThat(feedClient.reads).isEqualTo(1);
        assertThat(news.path("source").asText()).isEqualTo("Wowhead");
        assertThat(news.path("items")).hasSize(3);

        clock.advance(Duration.ofMinutes(1));
        service.getNews();

        assertThat(feedClient.reads).isEqualTo(2);
    }

    @Test
    void keepsTheLastGoodNewsWhenTheFeedFails() {
        String good = service.getNews();
        feedClient.failing = true;
        clock.advance(Duration.ofMinutes(5));

        assertThat(service.getNews()).isEqualTo(good);
        assertThat(service.getNews()).isEqualTo(good);
        assertThat(feedClient.reads).isEqualTo(2);
    }

    @Test
    void answersUnavailableUntilAReadWorks() {
        feedClient.failing = true;

        assertThatThrownBy(service::getNews).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(service::getNews).isInstanceOf(ResponseStatusException.class);
        assertThat(feedClient.reads).isEqualTo(1);

        feedClient.failing = false;
        clock.advance(Duration.ofMinutes(5));

        assertThat(service.getNews()).contains("383641");
    }

    private static final class FakeFeedClient extends NewsFeedClient {

        private int reads;
        private boolean failing;

        @Override
        public byte[] fetch(String url, String accept) throws IOException {
            reads++;

            if (failing) {
                throw new IOException("offline");
            }

            return NewsFeedParserTest.feed();
        }
    }

    private static final class MovableClock extends Clock {

        private Instant now = Instant.parse("2026-10-28T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
