package com.adrianpmdev.pokemon.features.tournaments.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentParticipantCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentParticipantUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentParticipantResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;

/**
 * Mapping utility for converting tournament participant entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class TournamentParticipantMapper {

    private TournamentParticipantMapper() {
    }

    public static TournamentParticipantResponse toResponse(TournamentParticipant participant) {
        return new TournamentParticipantResponse(
                participant.getId(),
                getTournamentId(participant.getTournament()),
                participant.getUserId(),
                participant.getJoinedAt(),
                participant.getStatus()
        );
    }

    public static TournamentParticipant toEntity(
            TournamentParticipantCreateRequest request,
            Tournament tournament
    ) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setTournament(tournament);
        participant.setUserId(request.userId());
        participant.setStatus(request.status());
        return participant;
    }

    public static void updateEntity(
            TournamentParticipant participant,
            TournamentParticipantUpdateRequest request,
            Tournament tournament
    ) {
        if (tournament != null) {
            participant.setTournament(tournament);
        }

        if (request.userId() != null) {
            participant.setUserId(request.userId());
        }

        if (request.status() != null) {
            participant.setStatus(request.status());
        }
    }

    private static UUID getTournamentId(Tournament tournament) {
        return tournament == null ? null : tournament.getId();
    }
}