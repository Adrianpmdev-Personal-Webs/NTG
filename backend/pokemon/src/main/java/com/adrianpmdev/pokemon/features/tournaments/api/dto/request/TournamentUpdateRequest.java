package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for updating tournaments.
 *
 * @author adrianpmdev
 */
public record TournamentUpdateRequest(
        UUID lockeId,
        String name,
        String description,
        String tournamentType,
        String status,
        Short maxParticipants,
        Instant startDate,
        Instant endDate
) {}