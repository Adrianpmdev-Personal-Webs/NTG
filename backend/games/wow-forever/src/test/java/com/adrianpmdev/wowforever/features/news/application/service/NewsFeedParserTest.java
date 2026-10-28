package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.news.api.dto.response.NewsResponses.NewsItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The RSS reader: category filter, limit, clean summaries, dates and links, and a feed that tries to load an external
 * entity.
 *
 * @author adrianpmdev
 */
class NewsFeedParserTest {

    @Test
    void keepsTheCategoryInFeedOrder() throws Exception {
        List<NewsItem> items = NewsFeedParser.parse(feed(), "forever", 15);

        assertThat(items).extracting(NewsItem::id).containsExactly(383641L, 383620L, 383630L);
    }

    @Test
    void readsEveryFieldOfAnItem() throws Exception {
        NewsItem item = NewsFeedParser.parse(feed(), "Forever", 1).getFirst();

        assertThat(item.title()).isEqualTo("New Class and Item Tuning on WoW: Forever - Hunter Changes");
        assertThat(item.summary()).isEqualTo(
                "With name reservations now live in WoW: Forever, there are some tuning changes for Hunters & an "
                        + "Alchemy potion.");
        assertThat(item.publishedAt()).isEqualTo("2026-10-28T21:17:17Z");
        assertThat(item.image()).isEqualTo("https://wow.zamimg.com/uploads/blog/images/55948-hunter-changes.jpg");
        assertThat(item.url()).startsWith("https://www.wowhead.com/news=383641/");
    }

    @Test
    void decodesEntitiesAndAllowsItemsWithoutPictureOrSummary() throws Exception {
        List<NewsItem> items = NewsFeedParser.parse(feed(), "Forever", 15);

        assertThat(items.get(1).title()).isEqualTo("New Player’s Starter Guide for World of Warcraft: Forever");
        assertThat(items.get(1).image()).isNull();
        assertThat(items.get(2).summary()).isNull();
    }

    @Test
    void stopsAtTheLimit() throws Exception {
        assertThat(NewsFeedParser.parse(feed(), "Forever", 2)).hasSize(2);
    }

    @Test
    void refusesExternalEntities() {
        String hostile = "<?xml version=\"1.0\"?><!DOCTYPE rss [<!ENTITY secret SYSTEM \"file:///etc/passwd\">]>"
                + "<rss><channel><item><title>&secret;</title></item></channel></rss>";

        assertThatThrownBy(() -> NewsFeedParser.parse(hostile.getBytes(StandardCharsets.UTF_8), null, 15))
                .isInstanceOf(IOException.class);
    }

    @Test
    void refusesWhatIsNotXml() {
        assertThatThrownBy(() -> NewsFeedParser.parse("<html".getBytes(StandardCharsets.UTF_8), null, 15))
                .isInstanceOf(IOException.class);
    }

    static byte[] feed() throws IOException {
        try (InputStream input = NewsFeedParserTest.class.getResourceAsStream("/news/feed.xml")) {
            return input.readAllBytes();
        }
    }
}
