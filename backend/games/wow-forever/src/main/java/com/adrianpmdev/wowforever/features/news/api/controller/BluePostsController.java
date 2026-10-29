package com.adrianpmdev.wowforever.features.news.api.controller;

import com.adrianpmdev.platform.web.JsonResponses;
import com.adrianpmdev.wowforever.features.news.application.service.BluePostsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the blue posts of WoW Forever (read only), taken from the Blizzard tracker that config.json
 * names.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping(value = "/api/v1/wow-forever/blue-posts", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BluePostsController {

    /** The browser keeps the posts but checks their ETag on every request (a 304 with no body if they are the same). */
    private static final CacheControl CACHE = CacheControl.noCache().cachePrivate();

    private final BluePostsService bluePostsService;

    /** Latest blue posts, newest first, with the credit of their source. */
    @GetMapping
    public ResponseEntity<String> getBluePosts() {
        return JsonResponses.cacheable(bluePostsService.getBluePosts(), CACHE);
    }
}
