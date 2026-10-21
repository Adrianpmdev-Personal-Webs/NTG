package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for tournament participant data.
 *
 * @author adrianpmdev
 */
public record TournamentParticipantResponse(
        UUID id,
        UUID tournamentId,
        UUID userId,
        Instant joinedAt,
        String status
) {}