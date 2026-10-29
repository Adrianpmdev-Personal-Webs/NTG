package com.adrianpmdev.wowforever.features.gamedata.api.controller;

import com.adrianpmdev.platform.web.JsonResponses;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameImagesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for the game data (talents, spellbooks, racials and the Legacy System), already turned into the models
 * every web client shows (see GameDataResponses). Read only: the data changes when a new game build comes out (see
 * GameDataSyncService), not from the app. One endpoint per screen, so each screen downloads only what it shows.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping(value = "/api/v1/wow-forever/data", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class GameDataController {

    /**
     * The data changes with each new game build, and players must see it right after: the browser keeps it but checks
     * its ETag on every request (a 304 with no body if unchanged).
     */
    private static final CacheControl CACHE = CacheControl.noCache().cachePrivate();

    private final GameDataService gameDataService;
    private final GameImagesService gameImagesService;

    /** Credit, classes, tools of the home, faction names and the levels that give a talent point. */
    @GetMapping
    public ResponseEntity<String> getIndex() {
        return json(gameDataService.getIndex());
    }

    /** Talent trees of a class, by slug (for example {@code warrior}). */
    @GetMapping("/talents/{classSlug}")
    public ResponseEntity<String> getTalents(@PathVariable String classSlug) {
        return json(gameDataService.getTalents(classSlug));
    }

    /** Spellbook of a class, by slug, with the text of every rank. */
    @GetMapping("/spellbook/{classSlug}")
    public ResponseEntity<String> getSpellbook(@PathVariable String classSlug) {
        return json(gameDataService.getSpellbook(classSlug));
    }

    /** Races of both factions with their racial abilities. */
    @GetMapping("/racials")
    public ResponseEntity<String> getRacials() {
        return json(gameDataService.getRacials());
    }

    /** Legacy System perk trees and challenges. */
    @GetMapping("/legacy")
    public ResponseEntity<String> getLegacy() {
        return json(gameDataService.getLegacy());
    }

    /**
     * Public URLs of the pictures of the area (logo, faction backgrounds and talent trees), by short key, for example
     * {@code backgrounds/horde} or {@code talents/warrior-arms}. Pictures not uploaded yet are left out. Not cached:
     * an admin may replace a picture at any time.
     */
    @GetMapping("/images")
    public Map<String, String> getImages() {
        return gameImagesService.getUrls();
    }

    private static ResponseEntity<String> json(String body) {
        return JsonResponses.cacheable(body, CACHE);
    }
}
