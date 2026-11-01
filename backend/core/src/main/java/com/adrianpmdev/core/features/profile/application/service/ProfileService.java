package com.adrianpmdev.core.features.profile.application.service;

import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.core.features.catalog.application.service.GameCatalogService;
import com.adrianpmdev.core.features.profile.api.dto.response.ProfileResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.features.users.application.service.UserService;
import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.platform.profile.ProfileBlockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The profile of the caller: their account and what each game they can open has about them. A game that fails is shown
 * without data, so it never breaks the profile.
 *
 * @author adrianpmdev
 */
@Service
public class ProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileService.class);

    private final UserService userService;
    private final GameCatalogService gameCatalogService;
    private final ObjectProvider<ProfileBlockProvider> providers;

    /**
     * Builds the service with its dependencies.
     *
     * @param userService        the accounts
     * @param gameCatalogService which games the caller can open
     * @param providers          games with something to show
     */
    public ProfileService(UserService userService, GameCatalogService gameCatalogService,
                          ObjectProvider<ProfileBlockProvider> providers) {
        this.userService = userService;
        this.gameCatalogService = gameCatalogService;
        this.providers = providers;
    }

    /**
     * The profile of the caller.
     *
     * @param userId      id of the caller
     * @param admin       whether the caller is an admin
     * @param authorities authorities of the caller
     * @return the profile
     */
    public ProfileResponse getOwn(UUID userId, boolean admin, Collection<String> authorities) {
        UserResponse user = userService.getById(userId);
        List<ProfileBlockProvider> gameProviders = providers.orderedStream().toList();
        List<ProfileResponse.ProfileGame> games = new ArrayList<>();

        for (GameResponse game : gameCatalogService.getVisible(admin, authorities)) {
            if (game.accessible() && !game.upcoming()) {
                ProfileBlock block = blockOf(game.id(), userId, gameProviders);

                games.add(new ProfileResponse.ProfileGame(game.id(), game.name(), game.iconUrl(), game.coverUrl(),
                        block.summary(), block.sections()));
            }
        }

        games.sort(Comparator.comparing(game -> game.summary().isEmpty() && game.sections().isEmpty()));

        return new ProfileResponse(user.username(), user.email(), user.avatarUrl(), user.role(), user.createdAt(), games);
    }

    private static ProfileBlock blockOf(String gameKey, UUID userId, List<ProfileBlockProvider> gameProviders) {
        for (ProfileBlockProvider provider : gameProviders) {
            if (provider.gameKey().equals(gameKey)) {
                try {
                    Optional<ProfileBlock> block = provider.block(userId);

                    if (block.isPresent()) {
                        return block.get();
                    }
                } catch (RuntimeException exception) {
                    LOG.warn("The game {} could not tell its profile block: {}", gameKey, exception.getMessage());
                }
            }
        }

        return new ProfileBlock(List.of(), List.of());
    }
}
