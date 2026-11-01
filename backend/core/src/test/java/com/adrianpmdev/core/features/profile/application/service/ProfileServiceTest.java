package com.adrianpmdev.core.features.profile.application.service;

import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.core.features.catalog.application.service.GameCatalogService;
import com.adrianpmdev.core.features.profile.api.dto.response.ProfileResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.features.users.application.service.UserService;
import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.platform.profile.ProfileBlockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A profile only tells the games the caller can open, and only about the caller.
 *
 * @author adrianpmdev
 */
class ProfileServiceTest {

    private static final UUID CALLER = UUID.randomUUID();
    private static final ProfileBlock BLOCK = new ProfileBlock(List.of(new ProfileBlock.Stat("Puntos", "12")),
            List.of(new ProfileBlock.Entry("Thrall", "Nivel 60")));

    private final UserService users = mock(UserService.class);
    private final GameCatalogService catalog = mock(GameCatalogService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ProfileBlockProvider> providers = mock(ObjectProvider.class);

    private final ProfileService service = new ProfileService(users, catalog, providers);

    @BeforeEach
    void setUp() {
        when(users.getById(CALLER)).thenReturn(new UserResponse(CALLER, "ana@example.com", "Ana", null, "USER",
                List.of("pokemon"), Instant.EPOCH));
    }

    @Test
    void theAccountOfTheCallerIsTold() {
        visibleGames();
        providers();

        ProfileResponse profile = own();

        assertThat(profile.username()).isEqualTo("Ana");
        assertThat(profile.email()).isEqualTo("ana@example.com");
        assertThat(profile.createdAt()).isEqualTo(Instant.EPOCH);
        assertThat(profile.games()).isEmpty();
    }

    @Test
    void onlyTheGamesTheCallerCanOpenAreShown() {
        visibleGames(game("pokemon", true, false), game("wow-forever", false, false), game("lol", true, true));
        providers(provider("pokemon", user -> Optional.of(BLOCK)), provider("wow-forever", user -> Optional.of(BLOCK)),
                provider("lol", user -> Optional.of(BLOCK)));

        assertThat(own().games()).extracting(ProfileResponse.ProfileGame::id).containsExactly("pokemon");
    }

    @Test
    void theBlockIsAskedAboutTheCaller() {
        visibleGames(game("pokemon", true, false));
        providers(provider("pokemon", user -> user.equals(CALLER) ? Optional.of(BLOCK) : Optional.empty()));

        ProfileResponse.ProfileGame game = own().games().getFirst();

        assertThat(game.stats()).isEqualTo(BLOCK.stats());
        assertThat(game.entries()).isEqualTo(BLOCK.entries());
    }

    @Test
    void aGameWithNothingAboutTheCallerIsShownEmpty() {
        visibleGames(game("pokemon", true, false), game("wow-forever", true, false));
        providers(provider("pokemon", user -> Optional.empty()));

        assertThat(own().games()).hasSize(2).allSatisfy(game -> {
            assertThat(game.stats()).isEmpty();
            assertThat(game.entries()).isEmpty();
        });
    }

    @Test
    void aFailingGameNeverBreaksTheProfile() {
        visibleGames(game("pokemon", true, false), game("wow-forever", true, false));
        providers(provider("pokemon", user -> {
            throw new IllegalStateException("down");
        }), provider("wow-forever", user -> Optional.of(BLOCK)));

        List<ProfileResponse.ProfileGame> games = own().games();

        assertThat(games.get(0).stats()).isEmpty();
        assertThat(games.get(1).stats()).isEqualTo(BLOCK.stats());
    }

    private ProfileResponse own() {
        return service.getOwn(CALLER, false, Set.of());
    }

    private void visibleGames(GameResponse... games) {
        when(catalog.getVisible(false, Set.of())).thenReturn(List.of(games));
    }

    private void providers(ProfileBlockProvider... list) {
        when(providers.orderedStream()).thenAnswer(call -> Stream.of(list));
    }

    private static GameResponse game(String id, boolean accessible, boolean upcoming) {
        return new GameResponse(id, id, null, null, upcoming, accessible, 1);
    }

    private static ProfileBlockProvider provider(String key, Function<UUID, Optional<ProfileBlock>> answer) {
        return new ProfileBlockProvider() {
            @Override
            public String gameKey() {
                return key;
            }

            @Override
            public Optional<ProfileBlock> block(UUID userId) {
                return answer.apply(userId);
            }
        };
    }
}
