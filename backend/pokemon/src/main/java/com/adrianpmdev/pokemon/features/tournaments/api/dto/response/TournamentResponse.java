package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for tournament data.
 *
 * @author adrianpmdev
 */
public record TournamentResponse(
        UUID id,
        UUID lockeId,
        String name,
        String description,
        String tournamentType,
        String status,
        Short maxParticipants,
        Instant startDate,
        Instant endDate,
        Instant createdAt
) {}