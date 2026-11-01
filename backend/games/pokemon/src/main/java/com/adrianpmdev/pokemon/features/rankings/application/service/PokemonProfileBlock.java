package com.adrianpmdev.pokemon.features.rankings.application.service;

import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.platform.profile.ProfileBlockProvider;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeDeathRepository;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.repository.PlayerRepository;
import com.adrianpmdev.pokemon.features.rankings.api.dto.response.PlayerStandingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What the Pokémon module shows on the profile of a player: their place in the all-time ranking and the Pokémon they
 * lost. An account without a linked player has nothing to show.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class PokemonProfileBlock implements ProfileBlockProvider {

    private final PlayerRepository playerRepository;
    private final RankingService rankingService;
    private final LockeDeathRepository lockeDeathRepository;

    @Override
    public String gameKey() {
        return "pokemon";
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfileBlock> block(UUID userId) {
        return playerRepository.findByUserId(userId).map(this::blockOf);
    }

    private ProfileBlock blockOf(Player player) {
        Optional<PlayerStandingResponse> standing = rankingService.globalRanking().stream()
                .filter(row -> row.playerId().equals(player.getId()))
                .findFirst();

        return new ProfileBlock(List.of(
                stat("Puesto histórico", standing.map(row -> "#" + row.position()).orElse("—")),
                stat("Puntos", standing.map(row -> "" + row.points()).orElse("0")),
                stat("Títulos", standing.map(row -> "" + row.titles()).orElse("0")),
                stat("Torneos", standing.map(row -> "" + row.tournaments()).orElse("0")),
                stat("Series", standing.map(row -> row.wins() + " - " + row.losses()).orElse("0 - 0")),
                stat("Pokémon muertos", "" + lockeDeathRepository.countByPlayerId(player.getId()))
        ), List.of());
    }

    private static ProfileBlock.Stat stat(String label, String value) {
        return new ProfileBlock.Stat(label, value);
    }
}
