package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** 
 * 
 * 
 * @author adrianpmdev
 */
public record GroupParticipantCreateRequest(
        @NotNull
        UUID groupId,

        @NotNull
        UUID tournamentParticipantId,

        Short groupPosition,

        @Min(0)
        Short wins,

        @Min(0)
        Short losses,

        @Min(0)
        Short points
) {}