package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Adds a player to a tournament.
 *
 * @param playerId player id
 *
 * @author adrianpmdev
 */
public record TournamentPlayerAddRequest(@NotNull UUID playerId) {
}
