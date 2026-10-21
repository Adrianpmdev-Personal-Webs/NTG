package com.adrianpmdev.pokemon.features.rankings.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for global standing data.
 *
 * @author adrianpmdev
 */
public record GlobalStandingResponse(
        UUID id,
        UUID userId,
        Short totalTournaments,
        Short tournamentsWon,
        Integer totalPoints,
        Short rankingPosition,
        Instant updatedAt
) {}