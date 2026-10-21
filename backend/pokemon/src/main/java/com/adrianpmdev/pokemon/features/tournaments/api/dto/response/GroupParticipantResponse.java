package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.util.UUID;

/**
 * Response DTO for group participant data.
 *
 * @author adrianpmdev
 */
public record GroupParticipantResponse(
        UUID id,
        UUID groupId,
        UUID tournamentParticipantId,
        Short groupPosition,
        Short wins,
        Short losses,
        Short points
) {}