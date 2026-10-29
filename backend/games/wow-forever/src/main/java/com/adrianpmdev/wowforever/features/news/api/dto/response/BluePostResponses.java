package com.adrianpmdev.wowforever.features.news.api.dto.response;

import java.util.List;

/**
 * Responses of the WoW Forever blue posts API: the latest posts of Blizzard employees about the game on the official
 * forums, as they write them (in English), plus the credit of the source.
 *
 * @author adrianpmdev
 */
public final class BluePostResponses {

    private BluePostResponses() {
    }

    /**
     * The blue posts of the launcher.
     *
     * @param source    name of the source, shown as its credit
     * @param sourceUrl page of the source the credit links to
     * @param items     latest posts, newest first
     */
    public record BluePostFeed(String source, String sourceUrl, List<BluePost> items) {
    }

    /**
     * A blue post.
     *
     * @param id          id of the post in the forums
     * @param title       title of its topic
     * @param reply       true when it answers inside the topic instead of opening it
     * @param author      forum name of the Blizzard employee
     * @param summary     start of the post without markup, or null
     * @param publishedAt when it was posted, as an ISO-8601 instant ({@code 2026-10-29T21:00:50.143Z})
     * @param url         the post in the forums
     */
    public record BluePost(long id, String title, boolean reply, String author, String summary, String publishedAt,
                           String url) {
    }
}
