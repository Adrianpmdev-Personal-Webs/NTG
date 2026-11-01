package com.adrianpmdev.platform.profile;

import java.util.Optional;
import java.util.UUID;

/**
 * Implemented by a game with something to show on the profile of a player. Core collects every provider and only asks
 * those of the games the player can open, so it never names a game. It is asked each time a player opens their profile.
 *
 * @author adrianpmdev
 */
public interface ProfileBlockProvider {

    /**
     * The game it speaks for.
     *
     * @return key of the game in the catalog
     */
    String gameKey();

    /**
     * What the game has about a player.
     *
     * @param userId platform user id of the player
     * @return the block, or empty when the game has nothing about them
     */
    Optional<ProfileBlock> block(UUID userId);
}
