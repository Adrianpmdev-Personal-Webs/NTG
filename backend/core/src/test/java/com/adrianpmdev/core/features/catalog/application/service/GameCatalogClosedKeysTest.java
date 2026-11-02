package com.adrianpmdev.core.features.catalog.application.service;

import com.adrianpmdev.core.features.assets.application.service.AssetService;
import com.adrianpmdev.core.features.catalog.infrastructure.persistence.entity.CatalogGame;
import com.adrianpmdev.core.features.catalog.infrastructure.persistence.repository.CatalogGameRepository;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.repository.TagRepository;
import com.adrianpmdev.platform.module.GameModule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The tag of a game only opens it when the game is released: not while it is coming soon, nor when its entry was removed.
 *
 * @author adrianpmdev
 */
class GameCatalogClosedKeysTest {

    private final CatalogGameRepository repository = mock(CatalogGameRepository.class);
    private final GameCatalogService service = new GameCatalogService(repository, mock(TagRepository.class),
            mock(AssetService.class), List.of(GameModule.of("released", "/api/v1/released"),
            GameModule.of("soon", "/api/v1/soon"), GameModule.of("removed", "/api/v1/removed")));

    @Test
    void onlyAReleasedGameIsOpen() {
        List<CatalogGame> games = List.of(game("released", false), game("soon", true));
        when(repository.findAllByOrderBySortOrderAscKeyAsc()).thenReturn(games);

        assertThat(service.closedKeys()).containsExactlyInAnyOrder("soon", "removed");
    }

    private static CatalogGame game(String key, boolean upcoming) {
        CatalogGame game = mock(CatalogGame.class);
        when(game.getKey()).thenReturn(key);
        when(game.getName()).thenReturn(key);
        when(game.isUpcoming()).thenReturn(upcoming);

        return game;
    }
}
