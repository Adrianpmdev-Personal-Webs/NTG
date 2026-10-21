package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import java.util.UUID;

/**
 * Request DTO for updating tournament participants.
 *
 * @author adrianpmdev
 */
public record TournamentParticipantUpdateRequest(
        UUID tournamentId,
        UUID userId,
        String status
) {}