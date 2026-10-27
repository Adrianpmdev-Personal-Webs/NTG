package com.adrianpmdev.pokemon.features.players.application.service;

import com.adrianpmdev.pokemon.features.players.api.dto.request.PlayerRequest;
import com.adrianpmdev.pokemon.features.players.api.dto.response.PlayerResponse;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.repository.PlayerRepository;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import com.adrianpmdev.pokemon.shared.port.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Players: list, create, replace and delete. A player who has played a tournament cannot be deleted
 * (the database refuses it and the API answers 409).
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final CurrentUser currentUser;

    /**
     * Every player by name. The linked account is only included for admins.
     *
     * @return players
     */
    @Transactional(readOnly = true)
    public List<PlayerResponse> list() {
        boolean admin = currentUser.isAdmin();

        return playerRepository.findAllByOrderByNameAsc().stream()
                .map(player -> toResponse(player, admin))
                .toList();
    }

    /**
     * Creates a player.
     *
     * @param request player data
     * @return the new player
     */
    @Transactional
    public PlayerResponse create(PlayerRequest request) {
        String name = request.name().trim();

        if (playerRepository.existsByNameIgnoreCase(name)) {
            throw ApiErrors.conflict("Ya existe un jugador con ese nombre");
        }

        Player player = new Player();
        player.setName(name);
        player.setUserId(request.userId());

        return toResponse(playerRepository.saveAndFlush(player), true);
    }

    /**
     * Replaces the data of a player.
     *
     * @param id      player id
     * @param request player data
     * @return the updated player
     */
    @Transactional
    public PlayerResponse update(UUID id, PlayerRequest request) {
        Player player = playerRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Player"));
        String name = request.name().trim();

        if (playerRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiErrors.conflict("Ya existe un jugador con ese nombre");
        }

        player.setName(name);
        player.setUserId(request.userId());

        // Flush so an account already linked to another player fails here, as a 409.
        return toResponse(playerRepository.saveAndFlush(player), true);
    }

    /**
     * Deletes a player who never played.
     *
     * @param id player id
     */
    @Transactional
    public void delete(UUID id) {
        playerRepository.delete(playerRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Player")));
        playerRepository.flush();
    }

    private static PlayerResponse toResponse(Player player, boolean withAccount) {
        return new PlayerResponse(player.getId(), player.getName(), withAccount ? player.getUserId() : null);
    }
}
