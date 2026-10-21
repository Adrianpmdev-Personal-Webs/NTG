package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import java.util.UUID;

/**
 * Request DTO for updating tournament groups.
 *
 * @author adrianpmdev
 */
public record TournamentGroupUpdateRequest(
        UUID tournamentId,
        String groupLetter,
        String status
) {}