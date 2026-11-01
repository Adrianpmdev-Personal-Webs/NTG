package com.adrianpmdev.core.features.profile.api.dto.response;

import com.adrianpmdev.platform.profile.ProfileBlock;

import java.time.Instant;
import java.util.List;

/**
 * The profile of the caller.
 *
 * @param username  name of the account
 * @param email     email of the account
 * @param avatarUrl avatar, or null
 * @param role      role of the account
 * @param createdAt when the account was created
 * @param games     the games the caller can open, in the order of the catalog
 *
 * @author adrianpmdev
 */
public record ProfileResponse(
        String username,
        String email,
        String avatarUrl,
        String role,
        Instant createdAt,
        List<ProfileGame> games) {

    /**
     * A game on the profile.
     *
     * @param id      key of the game
     * @param name    name of the game
     * @param iconUrl icon of the game, or null
     * @param stats   figures of the player; empty when the game has nothing about them
     * @param entries things of the player; empty when the game has nothing about them
     */
    public record ProfileGame(String id, String name, String iconUrl, List<ProfileBlock.Stat> stats,
                              List<ProfileBlock.Entry> entries) {
    }
}
