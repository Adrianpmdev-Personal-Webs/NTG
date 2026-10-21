package com.adrianpmdev.pokemon.features.tournaments.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentGroupCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentGroupUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentGroupResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentGroup;

/**
 * Mapping utility for converting tournament group entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class TournamentGroupMapper {

    private TournamentGroupMapper() {
    }

    public static TournamentGroupResponse toResponse(TournamentGroup group) {
        return new TournamentGroupResponse(
                group.getId(),
                getTournamentId(group.getTournament()),
                group.getGroupLetter(),
                group.getStatus(),
                group.getCreatedAt()
        );
    }

    public static TournamentGroup toEntity(TournamentGroupCreateRequest request, Tournament tournament) {
        TournamentGroup group = new TournamentGroup();
        group.setTournament(tournament);
        group.setGroupLetter(request.groupLetter());
        group.setStatus(request.status());
        return group;
    }

    public static void updateEntity(
            TournamentGroup group,
            TournamentGroupUpdateRequest request,
            Tournament tournament
    ) {
        if (tournament != null) {
            group.setTournament(tournament);
        }

        if (request.groupLetter() != null) {
            group.setGroupLetter(request.groupLetter());
        }

        if (request.status() != null) {
            group.setStatus(request.status());
        }
    }

    private static UUID getTournamentId(Tournament tournament) {
        return tournament == null ? null : tournament.getId();
    }
}