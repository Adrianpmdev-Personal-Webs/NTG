package com.adrianpmdev.pokemon.features.tournaments.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketMatchCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketMatchUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.BracketMatchResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.BracketMatch;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.BracketStage;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;

/**
 * Mapping utility for converting bracket match entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class BracketMatchMapper {

    private BracketMatchMapper() {
    }

    public static BracketMatchResponse toResponse(BracketMatch match) {
        return new BracketMatchResponse(
                match.getId(),
                getTournamentId(match.getTournament()),
                getBracketStageId(match.getBracketStage()),
                match.getMatchNumber(),
                getParticipantId(match.getParticipant1()),
                getParticipantId(match.getParticipant2()),
                getParticipantId(match.getWinner()),
                match.getReplayUrl(),
                match.getStatus(),
                match.getScheduledAt(),
                match.getCompletedAt(),
                match.getCreatedAt());
    }

    public static BracketMatch toEntity(
            BracketMatchCreateRequest request,
            Tournament tournament,
            BracketStage bracketStage,
            TournamentParticipant participant1,
            TournamentParticipant participant2,
            TournamentParticipant winner) {
        BracketMatch match = new BracketMatch();
        match.setTournament(tournament);
        match.setBracketStage(bracketStage);
        match.setMatchNumber(request.matchNumber());
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
            BracketMatch match,
            BracketMatchUpdateRequest request,
            Tournament tournament,
            BracketStage bracketStage,
            TournamentParticipant participant1,
            TournamentParticipant participant2,
            TournamentParticipant winner) {
        if (tournament != null) {
            match.setTournament(tournament);
        }

        if (bracketStage != null) {
            match.setBracketStage(bracketStage);
        }

        if (request.matchNumber() != null) {
            match.setMatchNumber(request.matchNumber());
        }

        match.setParticipant1(participant1);
        match.setParticipant2(participant2);
        match.setWinner(winner);
        match.setReplayUrl(request.replayUrl());

        if (request.status() != null) {
            match.setStatus(request.status());
        }

        match.setScheduledAt(request.scheduledAt());
        match.setCompletedAt(request.completedAt());
    }

    private static UUID getTournamentId(Tournament tournament) {
        return tournament == null ? null : tournament.getId();
    }

    private static UUID getBracketStageId(BracketStage bracketStage) {
        return bracketStage == null ? null : bracketStage.getId();
    }

    private static UUID getParticipantId(TournamentParticipant participant) {
        return participant == null ? null : participant.getId();
    }
}