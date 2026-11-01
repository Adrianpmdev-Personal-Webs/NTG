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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What the Pokémon module shows on the profile of a player: their place in the all-time ranking, how their series went
 * and the Pokémon they lost. An account without a linked player has nothing to show.
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
        long deaths = lockeDeathRepository.countByPlayerId(player.getId());
        Optional<PlayerStandingResponse> found = rankingService.globalRanking().stream()
                .filter(row -> row.playerId().equals(player.getId()))
                .findFirst();

        if (found.isEmpty()) {
            return new ProfileBlock(List.of("Sin torneos jugados"), List.of(section(List.of(
                    stat("Pokémon muertos", deaths)), List.of())));
        }

        PlayerStandingResponse standing = found.get();
        int series = standing.wins() + standing.losses();
        List<ProfileBlock.Bar> bars = new ArrayList<>();

        if (series > 0) {
            bars.add(new ProfileBlock.Bar("Series ganadas", standing.wins(), series, null));
        }

        return new ProfileBlock(
                List.of("#" + standing.position() + " histórico", standing.points() + " puntos",
                        standing.titles() + " títulos"),
                List.of(section(List.of(
                        new ProfileBlock.Stat("Puesto histórico", "#" + standing.position()),
                        stat("Puntos", standing.points()),
                        stat("Títulos", standing.titles()),
                        stat("Torneos", standing.tournaments()),
                        stat("Randomlockes", standing.lockes()),
                        stat("Pokémon muertos", deaths)), bars)));
    }

    private static ProfileBlock.Section section(List<ProfileBlock.Stat> stats, List<ProfileBlock.Bar> bars) {
        return new ProfileBlock.Section("Histórico", stats, bars, List.of());
    }

    private static ProfileBlock.Stat stat(String label, long value) {
        return new ProfileBlock.Stat(label, String.valueOf(value));
    }
}
