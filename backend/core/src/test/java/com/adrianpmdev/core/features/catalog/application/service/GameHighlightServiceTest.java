package com.adrianpmdev.core.features.catalog.application.service;

import com.adrianpmdev.core.features.catalog.api.dto.response.GameHighlightResponse;
import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.platform.module.GameHighlightProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Who gets which highlight, and that a failing game never breaks the others.
 *
 * @author adrianpmdev
 */
class GameHighlightServiceTest {

    private final GameCatalogService catalog = mock(GameCatalogService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<GameHighlightProvider> providers = mock(ObjectProvider.class);

    private final GameHighlightService service = new GameHighlightService(catalog, providers);

    @Test
    void onlyTheGamesTheCallerCanOpenTellTheirHighlight() {
        visibleGames(new GameResponse("pokemon", "Pokemon", null, null, false, true, 1),
                new GameResponse("lol", "LoL", null, null, true, false, 2));
        providers(provider("pokemon", () -> Optional.of("Torneo en curso")),
                provider("lol", () -> Optional.of("Secret")),
                provider("wow-forever", () -> Optional.of("Build 1")));

        assertThat(service.getVisible(false, Set.of()))
                .containsExactly(new GameHighlightResponse("pokemon", "Torneo en curso"));
    }

    @Test
    void aGameWithNothingToTellIsLeftOut() {
        visibleGames(new GameResponse("pokemon", "Pokemon", null, null, false, true, 1));
        providers(provider("pokemon", Optional::empty));

        assertThat(service.getVisible(false, Set.of())).isEmpty();
    }

    @Test
    void aGameThatFailsIsLeftOutAndTheOthersAnswer() {
        visibleGames(new GameResponse("pokemon", "Pokemon", null, null, false, true, 1),
                new GameResponse("wow-forever", "WoW", null, null, false, true, 2));
        providers(provider("pokemon", () -> {
            throw new IllegalStateException("database down");
        }), provider("wow-forever", () -> Optional.of("Build 1")));

        assertThat(service.getVisible(false, Set.of()))
                .containsExactly(new GameHighlightResponse("wow-forever", "Build 1"));
    }

    private void visibleGames(GameResponse... games) {
        when(catalog.getVisible(false, Set.of())).thenReturn(List.of(games));
    }

    private void providers(GameHighlightProvider... list) {
        when(providers.orderedStream()).thenAnswer(call -> Stream.of(list));
    }

    private static GameHighlightProvider provider(String key, Supplier<Optional<String>> text) {
        return new GameHighlightProvider() {
            @Override
            public String gameKey() {
                return key;
            }

            @Override
            public Optional<String> highlight() {
                return text.get();
            }
        };
    }
}
