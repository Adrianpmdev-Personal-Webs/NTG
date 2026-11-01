package com.adrianpmdev.pokemon.features.lockes.application.service;

import com.adrianpmdev.platform.error.ApiErrors;
import com.adrianpmdev.platform.port.CurrentUser;
import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeDeathRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeDeathsResponse;
import com.adrianpmdev.pokemon.features.lockes.domain.PokemonNames;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.LockeDeath;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeDeathRepository;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeRepository;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.adrianpmdev.pokemon.shared.dto.RefResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Pokémon lost in a randomlocke. Every player with the tag can read them; a player adds and deletes their own, as
 * long as they are in the roster to add one, and an admin can correct anybody's.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class LockeDeathService {

    private final LockeRepository lockeRepository;
    private final LockeDeathRepository lockeDeathRepository;
    private final CurrentUser currentUser;

    /**
     * The Pokémon lost in a randomlocke, by player.
     *
     * @param lockeId locke id
     * @return totals and deaths, with what the caller may change
     */
    @Transactional(readOnly = true)
    public LockeDeathsResponse list(UUID lockeId) {
        Locke locke = find(lockeId);
        List<LockeDeath> deaths = lockeDeathRepository.findByLockeIdOrderByCreatedAtAsc(lockeId);
        Map<UUID, List<LockeDeathsResponse.Death>> deathsByPlayer = new HashMap<>();
        Set<Player> players = new LinkedHashSet<>(locke.getRoster());

        for (LockeDeath death : deaths) {
            players.add(death.getPlayer());
            deathsByPlayer.computeIfAbsent(death.getPlayer().getId(), id -> new ArrayList<>())
                    .add(new LockeDeathsResponse.Death(death.getId(), death.getPokemon()));
        }

        List<LockeDeathsResponse.PlayerDeaths> rows = players.stream()
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .map(player -> {
                    List<LockeDeathsResponse.Death> lost = deathsByPlayer.getOrDefault(player.getId(), List.of());
                    boolean canEdit = canEdit(player);

                    return new LockeDeathsResponse.PlayerDeaths(new RefResponse(player.getId(), player.getName()),
                            lost.size(), canEdit && locke.getRoster().contains(player), canEdit, lost);
                })
                .toList();

        return new LockeDeathsResponse(deaths.size(), rows);
    }

    /**
     * Adds a Pokémon a player of the roster lost.
     *
     * @param lockeId locke id
     * @param request the player and the name of the Pokémon
     * @return the deaths of the randomlocke
     */
    @Transactional
    public LockeDeathsResponse add(UUID lockeId, LockeDeathRequest request) {
        Locke locke = find(lockeId);
        Player player = locke.getRoster().stream()
                .filter(candidate -> candidate.getId().equals(request.playerId()))
                .findFirst()
                .orElseThrow(() -> ApiErrors.notFound("Player"));

        requireCanEdit(player);

        LockeDeath death = new LockeDeath();
        death.setLocke(locke);
        death.setPlayer(player);
        death.setPokemon(PokemonNames.normalize(request.pokemon()));
        lockeDeathRepository.saveAndFlush(death);

        return list(lockeId);
    }

    /**
     * Deletes a Pokémon from the list.
     *
     * @param lockeId locke id
     * @param deathId death id
     * @return the deaths of the randomlocke
     */
    @Transactional
    public LockeDeathsResponse delete(UUID lockeId, UUID deathId) {
        LockeDeath death = lockeDeathRepository.findByIdAndLockeId(deathId, lockeId)
                .orElseThrow(() -> ApiErrors.notFound("Death"));

        requireCanEdit(death.getPlayer());
        lockeDeathRepository.delete(death);
        lockeDeathRepository.flush();

        return list(lockeId);
    }

    private Locke find(UUID id) {
        return lockeRepository.findWithRosterById(id).orElseThrow(() -> ApiErrors.notFound("Locke"));
    }

    private boolean canEdit(Player player) {
        UUID userId = player.getUserId();

        return currentUser.isAdmin() || (userId != null && currentUser.id().map(userId::equals).orElse(false));
    }

    private void requireCanEdit(Player player) {
        if (!canEdit(player)) {
            throw ApiErrors.forbidden();
        }
    }
}
