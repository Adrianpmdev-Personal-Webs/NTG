package com.adrianpmdev.pokemon.features.tournaments.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupParticipantCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupParticipantUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.GroupParticipantResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.GroupParticipant;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentGroup;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;

/**
 * Mapping utility for converting group participant entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class GroupParticipantMapper {

    private GroupParticipantMapper() {
    }

    public static GroupParticipantResponse toResponse(GroupParticipant participant) {
        return new GroupParticipantResponse(
                participant.getId(),
                getGroupId(participant.getGroup()),
                getTournamentParticipantId(participant.getTournamentParticipant()),
                participant.getGroupPosition(),
                participant.getWins(),
                participant.getLosses(),
                participant.getPoints()
        );
    }

    public static GroupParticipant toEntity(
            GroupParticipantCreateRequest request,
            TournamentGroup group,
            TournamentParticipant tournamentParticipant
    ) {
        GroupParticipant participant = new GroupParticipant();
        participant.setGroup(group);
        participant.setTournamentParticipant(tournamentParticipant);
        participant.setGroupPosition(request.groupPosition());
        participant.setWins(getValueOrDefault(request.wins()));
        participant.setLosses(getValueOrDefault(request.losses()));
        participant.setPoints(getValueOrDefault(request.points()));
        return participant;
    }

    public static void updateEntity(
            GroupParticipant participant,
            GroupParticipantUpdateRequest request,
            TournamentGroup group,
            TournamentParticipant tournamentParticipant
    ) {
        if (group != null) {
            participant.setGroup(group);
        }

        if (tournamentParticipant != null) {
            participant.setTournamentParticipant(tournamentParticipant);
        }

        if (request.groupPosition() != null) {
            participant.setGroupPosition(request.groupPosition());
        }

        if (request.wins() != null) {
            participant.setWins(request.wins());
        }

        if (request.losses() != null) {
            participant.setLosses(request.losses());
        }

        if (request.points() != null) {
            participant.setPoints(request.points());
        }
    }

    private static UUID getGroupId(TournamentGroup group) {
        return group == null ? null : group.getId();
    }

    private static UUID getTournamentParticipantId(TournamentParticipant tournamentParticipant) {
        return tournamentParticipant == null ? null : tournamentParticipant.getId();
    }

    private static Short getValueOrDefault(Short value) {
        return value == null ? 0 : value;
    }
}