package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating tournament participants.
 *
 * @author adrianpmdev
 */
public record TournamentParticipantCreateRequest(
        @NotNull
        UUID tournamentId,

        @NotNull
        UUID userId,

        @NotBlank
        @Size(max = 20)
        String status
) {}