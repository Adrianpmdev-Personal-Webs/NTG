package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for creating tournaments.
 *
 * @author adrianpmdev
 */
public record TournamentCreateRequest(
        @NotNull
        UUID lockeId,

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotBlank
        @Size(max = 20)
        String tournamentType,

        @NotBlank
        @Size(max = 30)
        String status,

        Short maxParticipants,

        Instant startDate,

        Instant endDate
) {}