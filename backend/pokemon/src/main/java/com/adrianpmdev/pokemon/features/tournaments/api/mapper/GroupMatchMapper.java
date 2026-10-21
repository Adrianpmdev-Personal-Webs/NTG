package com.adrianpmdev.pokemon.features.tournaments.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupMatchCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupMatchUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.GroupMatchResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.GroupMatch;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentGroup;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;

/**
 * Mapping utility for converting group match entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class GroupMatchMapper {

    private GroupMatchMapper() {
    }

    public static GroupMatchResponse toResponse(GroupMatch match) {
        return new GroupMatchResponse(
                match.getId(),
                getGroupId(match.getGroup()),
                getParticipantId(match.getParticipant1()),
                getParticipantId(match.getParticipant2()),
                getParticipantId(match.getWinner()),
                match.getReplayUrl(),
                match.getStatus(),
                match.getScheduledAt(),
                match.getCompletedAt(),
                match.getCreatedAt()
        );
    }

    public static GroupMatch toEntity(
            GroupMatchCreateRequest request,
            TournamentGroup group,
            TournamentParticipant participant1,
            TournamentParticipant participant2,
            TournamentParticipant winner
    ) {
        GroupMatch match = new GroupMatch();
        match.setGroup(group);
        match.setParticipant1(participant1);
        match.setParticipant2(participant2);
        match.setWinner(winner);
        match.setReplayUrl(request.replayUrl());
        match.setStatus(request.status());
        match.setScheduledAt(request.scheduledAt());
        match.setCompletedAt(request.completedAt());
        return match;
    }

    public static void updateEntity(
            GroupMatch match,
            GroupMatchUpdateRequest request,
            TournamentGroup group,
            TournamentParticipant participant1,
            TournamentParticipant participant2,
            TournamentParticipant winner
    ) {
        if (group != null) {
            match.setGroup(group);
        }

        if (participant1 != null) {
            match.setParticipant1(participant1);
        }

        if (participant2 != null) {
            match.setParticipant2(participant2);
        }

        match.setWinner(winner);
        match.setReplayUrl(request.replayUrl());

        if (request.status() != null) {
            match.setStatus(request.status());
        }

        match.setScheduledAt(request.scheduledAt());
        match.setCompletedAt(request.completedAt());
    }

    private static UUID getGroupId(TournamentGroup group) {
        return group == null ? null : group.getId();
    }

    private static UUID getParticipantId(TournamentParticipant participant) {
        return participant == null ? null : participant.getId();
    }
}