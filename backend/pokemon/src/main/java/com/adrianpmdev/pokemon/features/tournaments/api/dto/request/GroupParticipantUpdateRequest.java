package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import java.util.UUID;

/**
 *
 * 
 * @author adrianpmdev
 */
public record GroupParticipantUpdateRequest(
        UUID groupId,
        UUID tournamentParticipantId,
        Short groupPosition,
        Short wins,
        Short losses,
        Short points
) {}