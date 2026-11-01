package com.adrianpmdev.pokemon.features.rankings.application.service;

import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeDeathRepository;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.repository.PlayerRepository;
import com.adrianpmdev.pokemon.features.rankings.api.dto.response.PlayerStandingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What the profile tells about a player of the Pokémon module.
 *
 * @author adrianpmdev
 */
class PokemonProfileBlockTest {

    private static final UUID ACCOUNT = UUID.randomUUID();

    private final PlayerRepository players = mock(PlayerRepository.class);
    private final RankingService ranking = mock(RankingService.class);
    private final LockeDeathRepository deaths = mock(LockeDeathRepository.class);
    private final PokemonProfileBlock provider = new PokemonProfileBlock(players, ranking, deaths);
    private final Player player = new Player();

    @BeforeEach
    void setUp() {
        player.setId(UUID.randomUUID());
        player.setName("Ana");
        when(players.findByUserId(ACCOUNT)).thenReturn(Optional.of(player));
        when(deaths.countByPlayerId(player.getId())).thenReturn(7L);
    }

    @Test
    void anAccountWithoutPlayerHasNothing() {
        UUID other = UUID.randomUUID();
        when(players.findByUserId(other)).thenReturn(Optional.empty());

        assertThat(provider.block(other)).isEmpty();
        Mockito.verifyNoInteractions(ranking);
    }

    @Test
    void thePlayerGetsTheirOwnRowOfTheRanking() {
        when(ranking.globalRanking()).thenReturn(List.of(
                new PlayerStandingResponse(UUID.randomUUID(), "Other", 1, 90, 30, 1, 8, 5, 3),
                new PlayerStandingResponse(player.getId(), "Ana", 2, 40, 20, 5, 6, 3, 2)));

        ProfileBlock block = provider.block(ACCOUNT).orElseThrow();
        ProfileBlock.Section section = block.sections().getFirst();

        assertThat(block.summary()).containsExactly("#2 histórico", "40 puntos", "3 títulos");
        assertThat(section.stats()).containsExactly(
                new ProfileBlock.Stat("Puesto histórico", "#2"),
                new ProfileBlock.Stat("Puntos", "40"),
                new ProfileBlock.Stat("Títulos", "3"),
                new ProfileBlock.Stat("Torneos", "6"),
                new ProfileBlock.Stat("Randomlockes", "2"),
                new ProfileBlock.Stat("Pokémon muertos", "7"));
        assertThat(section.bars()).containsExactly(new ProfileBlock.Bar("Series ganadas", 20, 25, null));
    }

    @Test
    void withoutSeriesThereIsNoBar() {
        when(ranking.globalRanking()).thenReturn(List.of(
                new PlayerStandingResponse(player.getId(), "Ana", 2, 0, 0, 0, 1, 0, 1)));

        assertThat(provider.block(ACCOUNT).orElseThrow().sections().getFirst().bars()).isEmpty();
    }

    @Test
    void aPlayerOutOfTheRankingStillShowsTheirDeaths() {
        when(ranking.globalRanking()).thenReturn(List.of());

        ProfileBlock block = provider.block(ACCOUNT).orElseThrow();

        assertThat(block.summary()).containsExactly("Sin torneos jugados");
        assertThat(block.sections().getFirst().stats())
                .containsExactly(new ProfileBlock.Stat("Pokémon muertos", "7"));
    }
}
