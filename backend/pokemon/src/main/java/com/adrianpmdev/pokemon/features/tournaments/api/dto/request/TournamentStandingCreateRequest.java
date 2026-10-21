package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for creating tournament standings.
 *
 * @author adrianpmdev
 */
public record TournamentStandingCreateRequest(
        @NotNull
        UUID tournamentId,

        @NotNull
        UUID tournamentParticipantId,

        Short finalPosition,

        @Min(0)
        Short totalWins,

        @Min(0)
        Short totalLosses,

        @Min(0)
        Short totalPoints
) {}