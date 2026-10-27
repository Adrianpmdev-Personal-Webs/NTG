package com.adrianpmdev.pokemon.features.lockes.api.dto.response;

import com.adrianpmdev.pokemon.features.rankings.api.dto.response.PlayerStandingResponse;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentSummaryResponse;
import com.adrianpmdev.pokemon.shared.dto.RefResponse;

import java.util.List;
import java.util.UUID;

/**
 * Page of a randomlocke.
 *
 * @param id          locke id
 * @param name        locke name
 * @param description description
 * @param rules       rules document, or null
 * @param games       games its tournaments were played in, in play order
 * @param roster      fixed players
 * @param tournaments tournaments in play order
 * @param standings   cumulative standings of the randomlocke
 *
 * @author adrianpmdev
 */
public record LockeDetailResponse(
        UUID id,
        String name,
        String description,
        Object rules,
        List<RefResponse> games,
        List<RefResponse> roster,
        List<TournamentSummaryResponse> tournaments,
        List<PlayerStandingResponse> standings
) {
}
