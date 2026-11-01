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
 * @param games     the games the caller can open: the ones with data first, each group in the order of the catalog
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
     * @param id       key of the game
     * @param name     name of the game
     * @param iconUrl  icon of the game, or null
     * @param coverUrl cover of the game, or null
     * @param summary  short texts for the overview; empty when the game has nothing about the caller
     * @param sections what its tab shows; empty when the game has nothing about the caller
     */
    public record ProfileGame(String id, String name, String iconUrl, String coverUrl, List<String> summary,
                              List<ProfileBlock.Section> sections) {
    }
}
