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
        PlayerStandingResponse other = mock(PlayerStandingResponse.class);
        when(other.playerId()).thenReturn(UUID.randomUUID());
        PlayerStandingResponse own = mock(PlayerStandingResponse.class);
        when(own.playerId()).thenReturn(player.getId());
        when(own.position()).thenReturn(2);
        when(own.points()).thenReturn(40);
        when(own.titles()).thenReturn(3);
        when(own.tournaments()).thenReturn(6);
        when(own.wins()).thenReturn(20);
        when(own.losses()).thenReturn(5);
        when(ranking.globalRanking()).thenReturn(List.of(other, own));

        assertThat(provider.block(ACCOUNT).orElseThrow().stats()).containsExactly(
                new ProfileBlock.Stat("Puesto histórico", "#2"),
                new ProfileBlock.Stat("Puntos", "40"),
                new ProfileBlock.Stat("Títulos", "3"),
                new ProfileBlock.Stat("Torneos", "6"),
                new ProfileBlock.Stat("Series", "20 - 5"),
                new ProfileBlock.Stat("Pokémon muertos", "7"));
    }

    @Test
    void aPlayerOutOfTheRankingStartsFromZero() {
        when(ranking.globalRanking()).thenReturn(List.of());

        assertThat(provider.block(ACCOUNT).orElseThrow().stats()).containsExactly(
                new ProfileBlock.Stat("Puesto histórico", "—"),
                new ProfileBlock.Stat("Puntos", "0"),
                new ProfileBlock.Stat("Títulos", "0"),
                new ProfileBlock.Stat("Torneos", "0"),
                new ProfileBlock.Stat("Series", "0 - 0"),
                new ProfileBlock.Stat("Pokémon muertos", "7"));
    }
}
