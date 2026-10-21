package com.adrianpmdev.pokemon.features.rankings.api.dto.request;

import java.util.UUID;

/**
 * Request DTO for updating global standings.
 *
 * @author adrianpmdev
 */
public record GlobalStandingUpdateRequest(
        UUID userId,
        Short totalTournaments,
        Short tournamentsWon,
        Integer totalPoints,
        Short rankingPosition
) {}