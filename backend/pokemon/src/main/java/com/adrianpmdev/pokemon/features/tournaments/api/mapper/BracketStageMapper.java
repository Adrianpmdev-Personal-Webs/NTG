package com.adrianpmdev.pokemon.features.tournaments.api.mapper;

import java.util.UUID;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketStageCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketStageUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.BracketStageResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.BracketStage;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;

/**
 * Mapping utility for converting bracket stage entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class BracketStageMapper {

    private BracketStageMapper() {
    }

    public static BracketStageResponse toResponse(BracketStage stage) {
        return new BracketStageResponse(
                stage.getId(),
                getTournamentId(stage.getTournament()),
                stage.getStageName(),
                stage.getStageOrder(),
                stage.getStatus(),
                stage.getCreatedAt()
        );
    }

    public static BracketStage toEntity(BracketStageCreateRequest request, Tournament tournament) {
        BracketStage stage = new BracketStage();
        stage.setTournament(tournament);
        stage.setStageName(request.stageName());
        stage.setStageOrder(request.stageOrder());
        stage.setStatus(request.status());
        return stage;
    }

    public static void updateEntity(
            BracketStage stage,
            BracketStageUpdateRequest request,
            Tournament tournament
    ) {
        if (tournament != null) {
            stage.setTournament(tournament);
        }

        if (request.stageName() != null) {
            stage.setStageName(request.stageName());
        }

        if (request.stageOrder() != null) {
            stage.setStageOrder(request.stageOrder());
        }

        if (request.status() != null) {
            stage.setStatus(request.status());
        }
    }

    private static UUID getTournamentId(Tournament tournament) {
        return tournament == null ? null : tournament.getId();
    }
}