package com.adrianpmdev.pokemon.features.tournaments.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentStandingCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentStandingUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentStandingResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentStanding;

/**
 * Mapping utility for converting tournament standing entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class TournamentStandingMapper {

    private TournamentStandingMapper() {
    }

    public static TournamentStandingResponse toResponse(TournamentStanding standing) {
        return new TournamentStandingResponse(
                standing.getId(),
                getTournamentId(standing.getTournament()),
                getTournamentParticipantId(standing.getTournamentParticipant()),
                standing.getFinalPosition(),
                standing.getTotalWins(),
                standing.getTotalLosses(),
                standing.getTotalPoints(),
                standing.getUpdatedAt()
        );
    }

    public static TournamentStanding toEntity(
            TournamentStandingCreateRequest request,
            Tournament tournament,
            TournamentParticipant tournamentParticipant
    ) {
        TournamentStanding standing = new TournamentStanding();
        standing.setTournament(tournament);
        standing.setTournamentParticipant(tournamentParticipant);
        standing.setFinalPosition(request.finalPosition());
        standing.setTotalWins(getValueOrDefault(request.totalWins()));
        standing.setTotalLosses(getValueOrDefault(request.totalLosses()));
        standing.setTotalPoints(getValueOrDefault(request.totalPoints()));
        return standing;
    }

    public static void updateEntity(
            TournamentStanding standing,
            TournamentStandingUpdateRequest request,
            Tournament tournament,
            TournamentParticipant tournamentParticipant
    ) {
        if (tournament != null) {
            standing.setTournament(tournament);
        }

        if (tournamentParticipant != null) {
            standing.setTournamentParticipant(tournamentParticipant);
        }

        if (request.finalPosition() != null) {
            standing.setFinalPosition(request.finalPosition());
        }

        if (request.totalWins() != null) {
            standing.setTotalWins(request.totalWins());
        }

        if (request.totalLosses() != null) {
            standing.setTotalLosses(request.totalLosses());
        }

        if (request.totalPoints() != null) {
            standing.setTotalPoints(request.totalPoints());
        }
    }

    private static UUID getTournamentId(Tournament tournament) {
        return tournament == null ? null : tournament.getId();
    }

    private static UUID getTournamentParticipantId(TournamentParticipant tournamentParticipant) {
        return tournamentParticipant == null ? null : tournamentParticipant.getId();
    }

    private static Short getValueOrDefault(Short value) {
        return value == null ? 0 : value;
    }
}