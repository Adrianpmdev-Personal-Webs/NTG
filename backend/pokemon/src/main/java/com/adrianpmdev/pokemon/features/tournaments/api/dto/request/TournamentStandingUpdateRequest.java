package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import java.util.UUID;

/**
 * Request DTO for updating tournament standings.
 *
 * @author adrianpmdev
 */
public record TournamentStandingUpdateRequest(
        UUID tournamentId,
        UUID tournamentParticipantId,
        Short finalPosition,
        Short totalWins,
        Short totalLosses,
        Short totalPoints
) {}