package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for tournament group data.
 *
 * @author adrianpmdev
 */
public record TournamentGroupResponse(
        UUID id,
        UUID tournamentId,
        String groupLetter,
        String status,
        Instant createdAt
) {}