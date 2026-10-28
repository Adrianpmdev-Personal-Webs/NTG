package com.adrianpmdev.wowforever.features.news.api.controller;

import com.adrianpmdev.platform.web.JsonResponses;
import com.adrianpmdev.wowforever.features.news.application.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the launcher news of WoW Forever (read only), taken from the feed that config.json names.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping(value = "/api/v1/wow-forever/news", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class NewsController {

    /** The browser keeps the news but checks its ETag on every request (a 304 with no body if they did not change). */
    private static final CacheControl CACHE = CacheControl.noCache().cachePrivate();

    private final NewsService newsService;

    /** Latest news, newest first, with the credit of their source. */
    @GetMapping
    public ResponseEntity<String> getNews() {
        return JsonResponses.cacheable(newsService.getNews(), CACHE);
    }
}
