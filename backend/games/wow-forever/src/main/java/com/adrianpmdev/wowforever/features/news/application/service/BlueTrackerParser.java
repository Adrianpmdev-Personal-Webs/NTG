package com.adrianpmdev.wowforever.features.news.application.service;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.news.api.dto.response.BluePostResponses.BluePost;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reads one page of the Blizzard tracker (the JSON list of a Discourse group's posts) and keeps the posts about WoW
 * Forever: those of its forum categories, and those of other categories whose topic title names the game. Only
 * regular posts are kept, never moderator notes, and their excerpt loses the links Discourse writes in place of
 * pictures and videos ({@code [title]}).
 *
 * @author adrianpmdev
 */
final class BlueTrackerParser {

    private static final int REGULAR_POST = 1;
    private static final Pattern MEDIA_LINK = Pattern.compile("(?is)<a\\b[^>]*>\\s*\\[[^\\]]*]\\s*</a>");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private BlueTrackerParser() {
    }

    /**
     * A page of the tracker.
     *
     * @param posts    posts about the game, newest first
     * @param oldestId id of the last post of the page, game or not, to ask for the next one; null when it was empty
     */
    record Page(List<BluePost> posts, Long oldestId) {
    }

    /**
     * Reads a page.
     *
     * @param json     the page
     * @param settings which posts are about the game and where the forums are
     * @return its posts about the game and where the next page starts
     * @throws IOException when the page is not the JSON of the tracker
     */
    static Page parse(byte[] json, GameDataConfig.BluePosts settings) throws IOException {
        JsonNode posts = OBJECT_MAPPER.readTree(json).path("posts");

        if (!posts.isArray()) {
            throw new IOException("The blue tracker answer has no posts");
        }

        List<BluePost> kept = new ArrayList<>();
        Long oldestId = null;

        for (JsonNode post : posts) {
            oldestId = post.path("id").isIntegralNumber() ? post.path("id").asLong() : oldestId;

            if (isAboutTheGame(post, settings)) {
                BluePost bluePost = toBluePost(post, settings.forumUrl());

                if (bluePost != null) {
                    kept.add(bluePost);
                }
            }
        }

        return new Page(List.copyOf(kept), oldestId);
    }

    private static boolean isAboutTheGame(JsonNode post, GameDataConfig.BluePosts settings) {
        if (post.path("post_type").asInt() != REGULAR_POST || post.path("hidden").asBoolean()
                || !post.path("deleted_at").isMissingNode() && !post.path("deleted_at").isNull()) {
            return false;
        }

        if (settings.categories().contains(post.path("category_id").asInt())) {
            return true;
        }

        String keyword = settings.titleKeyword();
        String title = post.path("topic_title").asText("");

        return keyword != null && !keyword.isBlank()
                && title.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private static BluePost toBluePost(JsonNode post, String forumUrl) {
        String title = HtmlText.text(post.path("topic_title").asText(null));
        String author = HtmlText.text(post.path("username").asText(null));
        String publishedAt = instant(post.path("created_at").asText(null));
        String path = post.path("url").asText("");

        if (!post.path("id").isIntegralNumber() || title == null || author == null || publishedAt == null
                || !path.startsWith("/")) {
            return null;
        }

        String excerpt = post.path("excerpt").asText(null);
        String summary = excerpt == null ? null : HtmlText.plain(MEDIA_LINK.matcher(excerpt).replaceAll(" "));

        return new BluePost(post.path("id").asLong(), title, post.path("post_number").asInt(1) > 1, author, summary,
                publishedAt, forumUrl + path);
    }

    private static String instant(String createdAt) {
        if (createdAt == null) {
            return null;
        }

        try {
            return Instant.parse(createdAt).toString();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
