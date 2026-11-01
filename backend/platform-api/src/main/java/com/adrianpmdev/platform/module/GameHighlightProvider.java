package com.adrianpmdev.platform.module;

import java.util.Optional;

/**
 * Implemented by a game with something current to tell on the home page, next to its cover. Core collects every
 * provider and only asks those of the games the user can open, so it never names a game. It is asked each time someone
 * opens the home page, so it must be cheap: an answer from memory or one small query.
 *
 * @author adrianpmdev
 */
public interface GameHighlightProvider {

    /**
     * The game it speaks for.
     *
     * @return key of the game in the catalog
     */
    String gameKey();

    /**
     * What is happening in the game right now, written for players.
     *
     * @return a short text, or empty when there is nothing to tell
     */
    Optional<String> highlight();
}
