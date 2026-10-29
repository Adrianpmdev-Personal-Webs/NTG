package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.news.api.dto.response.BluePostResponses.BluePost;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Blizzard tracker reader: which posts are about the game, the fields of a post, clean summaries and where the
 * next page starts.
 *
 * @author adrianpmdev
 */
class BlueTrackerParserTest {

    static final GameDataConfig.BluePosts SETTINGS = settings(20, 5);

    @Test
    void keepsThePostsAboutTheGameInTrackerOrder() throws Exception {
        BlueTrackerParser.Page page = BlueTrackerParser.parse(page(1), SETTINGS);

        assertThat(page.posts()).extracting(BluePost::id).containsExactly(105L, 103L, 102L);
        assertThat(page.oldestId()).isEqualTo(101L);
    }

    @Test
    void readsEveryFieldOfAPost() throws Exception {
        BluePost post = BlueTrackerParser.parse(page(1), SETTINGS).posts().getFirst();

        assertThat(post.title()).isEqualTo("World of Warcraft: Forever Goes Live Globally November 4!");
        assertThat(post.reply()).isFalse();
        assertThat(post.author()).isEqualTo("BlizzardEntertainment");
        assertThat(post.summary()).isEqualTo("Begin your first adventure in World of Warcraft: Forever, launching "
                + "worldwide November 4 at 3:00 pm PST. Adventure to level 60 a…");
        assertThat(post.publishedAt()).isEqualTo("2026-10-29T21:00:50.143Z");
        assertThat(post.url()).isEqualTo("https://us.forums.blizzard.com/en/wow"
                + "/t/world-of-warcraft-forever-goes-live-globally-november-4/2396008/1");
    }

    @Test
    void marksRepliesAndDropsPicturesAndVideosFromSummaries() throws Exception {
        List<BluePost> posts = BlueTrackerParser.parse(page(1), SETTINGS).posts();

        assertThat(posts.get(1).summary()).isNull();
        assertThat(posts.get(2).reply()).isTrue();
        assertThat(posts.get(2).summary()).startsWith("We’ve updated the previous post");
    }

    @Test
    void skipsHiddenPostsAndOtherGames() throws Exception {
        BlueTrackerParser.Page page = BlueTrackerParser.parse(page(2), SETTINGS);

        assertThat(page.posts()).extracting(BluePost::id).containsExactly(99L, 98L);
        assertThat(page.oldestId()).isEqualTo(96L);
    }

    @Test
    void anEmptyPageHasNoNextPage() throws Exception {
        BlueTrackerParser.Page page = BlueTrackerParser.parse(page(3), SETTINGS);

        assertThat(page.posts()).isEmpty();
        assertThat(page.oldestId()).isNull();
    }

    @Test
    void refusesWhatIsNotTheTracker() {
        assertThatThrownBy(() -> BlueTrackerParser.parse("{}".getBytes(StandardCharsets.UTF_8), SETTINGS))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> BlueTrackerParser.parse("<html".getBytes(StandardCharsets.UTF_8), SETTINGS))
                .isInstanceOf(IOException.class);
    }

    static GameDataConfig.BluePosts settings(int limit, int maxPages) {
        return new GameDataConfig.BluePosts("https://forums.test/tracker.json", "https://us.forums.blizzard.com/en/wow",
                List.of(346, 347, 348, 349), "Forever", limit, maxPages, 10, "Blizzard Forums",
                "https://forums.test/tracker");
    }

    static byte[] page(int number) throws IOException {
        String name = "/news/blue-tracker-page-" + number + ".json";

        try (InputStream input = BlueTrackerParserTest.class.getResourceAsStream(name)) {
            return input.readAllBytes();
        }
    }
}
