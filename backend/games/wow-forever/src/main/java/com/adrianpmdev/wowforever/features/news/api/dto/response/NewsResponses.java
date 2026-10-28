package com.adrianpmdev.wowforever.features.news.api.dto.response;

import java.util.List;

/**
 * Responses of the WoW Forever news API: the latest items of the source feed, as the feed writes them (in English),
 * plus the credit of the source.
 *
 * @author adrianpmdev
 */
public final class NewsResponses {

    private NewsResponses() {
    }

    /**
     * The news of the launcher.
     *
     * @param source    name of the source, shown as its credit
     * @param sourceUrl page of the source the credit links to
     * @param items     latest items, newest first
     */
    public record NewsFeed(String source, String sourceUrl, List<NewsItem> items) {
    }

    /**
     * A news item.
     *
     * @param id          id of the item in the source
     * @param title       title
     * @param summary     short text without markup, or null
     * @param publishedAt when it was published, as an ISO-8601 instant ({@code 2026-10-28T21:17:17Z})
     * @param image       picture URL, or null
     * @param url         the full article in the source
     */
    public record NewsItem(long id, String title, String summary, String publishedAt, String image, String url) {
    }
}
