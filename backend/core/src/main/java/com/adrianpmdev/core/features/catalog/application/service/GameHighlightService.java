package com.adrianpmdev.core.features.catalog.application.service;

import com.adrianpmdev.core.features.catalog.api.dto.response.GameHighlightResponse;
import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.platform.module.GameHighlightProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What each game tells on the home page (see {@link GameHighlightProvider}), only for the games the caller can open. A
 * game that fails to answer is left out, because a highlight is never worth a broken home page.
 *
 * @author adrianpmdev
 */
@Service
public class GameHighlightService {

    private static final Logger LOG = LoggerFactory.getLogger(GameHighlightService.class);

    private final GameCatalogService gameCatalogService;
    private final ObjectProvider<GameHighlightProvider> providers;

    /**
     * Builds the service with its dependencies.
     *
     * @param gameCatalogService which games the caller can open
     * @param providers          games with something to tell
     */
    public GameHighlightService(GameCatalogService gameCatalogService, ObjectProvider<GameHighlightProvider> providers) {
        this.gameCatalogService = gameCatalogService;
        this.providers = providers;
    }

    /**
     * The highlights of the games the caller can open.
     *
     * @param admin       whether the caller is an admin
     * @param authorities authorities of the caller
     * @return highlights, one at most per game
     */
    public List<GameHighlightResponse> getVisible(boolean admin, Collection<String> authorities) {
        Set<String> accessible = gameCatalogService.getVisible(admin, authorities).stream()
                .filter(GameResponse::accessible)
                .map(GameResponse::id)
                .collect(Collectors.toSet());
        List<GameHighlightResponse> highlights = new ArrayList<>();

        for (GameHighlightProvider provider : providers.orderedStream().toList()) {
            if (accessible.contains(provider.gameKey())) {
                try {
                    provider.highlight()
                            .ifPresent(text -> highlights.add(new GameHighlightResponse(provider.gameKey(), text)));
                } catch (RuntimeException exception) {
                    LOG.warn("The game {} could not tell its highlight: {}", provider.gameKey(), exception.getMessage());
                }
            }
        }

        return highlights;
    }
}
