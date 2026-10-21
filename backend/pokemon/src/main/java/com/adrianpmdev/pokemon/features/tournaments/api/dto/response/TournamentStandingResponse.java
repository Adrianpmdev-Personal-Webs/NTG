package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for tournament standing data.
 *
 * @author adrianpmdev
 */
public record TournamentStandingResponse(
        UUID id,
        UUID tournamentId,
        UUID tournamentParticipantId,
        Short finalPosition,
        Short totalWins,
        Short totalLosses,
        Short totalPoints,
        Instant updatedAt
) {}