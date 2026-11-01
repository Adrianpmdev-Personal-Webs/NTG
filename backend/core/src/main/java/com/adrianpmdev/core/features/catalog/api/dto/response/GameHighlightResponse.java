package com.adrianpmdev.core.features.catalog.api.dto.response;

/**
 * Something current about a game, for the home page.
 *
 * @param game key of the game
 * @param text what is happening, written for players
 *
 * @author adrianpmdev
 */
public record GameHighlightResponse(
    String game,
    String text
) {}
