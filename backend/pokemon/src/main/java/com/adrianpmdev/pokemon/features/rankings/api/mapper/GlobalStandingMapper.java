package com.adrianpmdev.pokemon.features.rankings.api.mapper;

import com.adrianpmdev.pokemon.features.rankings.api.dto.request.GlobalStandingCreateRequest;
import com.adrianpmdev.pokemon.features.rankings.api.dto.request.GlobalStandingUpdateRequest;
import com.adrianpmdev.pokemon.features.rankings.api.dto.response.GlobalStandingResponse;
import com.adrianpmdev.pokemon.features.rankings.infrastructure.persistence.entity.GlobalStanding;

/**
 * Mapping utility for converting global standing entities and DTOs.
 *
 * @author adrianpmdev
 */
public final class GlobalStandingMapper {

    private GlobalStandingMapper() {
    }

    public static GlobalStandingResponse toResponse(GlobalStanding standing) {
        return new GlobalStandingResponse(
                standing.getId(),
                standing.getUserId(),
                standing.getTotalTournaments(),
                standing.getTournamentsWon(),
                standing.getTotalPoints(),
                standing.getRankingPosition(),
                standing.getUpdatedAt()
        );
    }

    public static GlobalStanding toEntity(GlobalStandingCreateRequest request) {
        GlobalStanding standing = new GlobalStanding();
        standing.setUserId(request.userId());
        standing.setTotalTournaments(request.totalTournaments());
        standing.setTournamentsWon(request.tournamentsWon());
        standing.setTotalPoints(request.totalPoints());
        standing.setRankingPosition(request.rankingPosition());
        return standing;
    }

    public static void updateEntity(GlobalStanding standing, GlobalStandingUpdateRequest request) {
        if (request.userId() != null) {
            standing.setUserId(request.userId());
        }

        if (request.totalTournaments() != null) {
            standing.setTotalTournaments(request.totalTournaments());
        }

        if (request.tournamentsWon() != null) {
            standing.setTournamentsWon(request.tournamentsWon());
        }

        if (request.totalPoints() != null) {
            standing.setTotalPoints(request.totalPoints());
        }

        if (request.rankingPosition() != null) {
            standing.setRankingPosition(request.rankingPosition());
        }
    }
}