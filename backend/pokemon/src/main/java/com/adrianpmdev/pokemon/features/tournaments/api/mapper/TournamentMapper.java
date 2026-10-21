package com.adrianpmdev.pokemon.features.tournaments.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentResponse;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;

/**
 * Mapping utility for converting tournament entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class TournamentMapper {

    private TournamentMapper() {
    }

    public static TournamentResponse toResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                getLockeId(tournament.getLocke()),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getTournamentType(),
                tournament.getStatus(),
                tournament.getMaxParticipants(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getCreatedAt()
        );
    }

    public static Tournament toEntity(TournamentCreateRequest request, Locke locke) {
        Tournament tournament = new Tournament();
        tournament.setLocke(locke);
        tournament.setName(request.name());
        tournament.setDescription(request.description());
        tournament.setTournamentType(request.tournamentType());
        tournament.setStatus(request.status());
        tournament.setMaxParticipants(request.maxParticipants());
        tournament.setStartDate(request.startDate());
        tournament.setEndDate(request.endDate());
        return tournament;
    }

    public static void updateEntity(Tournament tournament, TournamentUpdateRequest request, Locke locke) {
        if (locke != null) {
            tournament.setLocke(locke);
        }

        if (request.name() != null) {
            tournament.setName(request.name());
        }

        if (request.description() != null) {
            tournament.setDescription(request.description());
        }

        if (request.tournamentType() != null) {
            tournament.setTournamentType(request.tournamentType());
        }

        if (request.status() != null) {
            tournament.setStatus(request.status());
        }

        if (request.maxParticipants() != null) {
            tournament.setMaxParticipants(request.maxParticipants());
        }

        if (request.startDate() != null) {
            tournament.setStartDate(request.startDate());
        }

        if (request.endDate() != null) {
            tournament.setEndDate(request.endDate());
        }
    }

    private static UUID getLockeId(Locke locke) {
        return locke == null ? null : locke.getId();
    }
}