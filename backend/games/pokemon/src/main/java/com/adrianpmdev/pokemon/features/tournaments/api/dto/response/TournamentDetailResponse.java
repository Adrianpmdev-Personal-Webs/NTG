package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import com.adrianpmdev.pokemon.shared.dto.RefResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Everything a tournament page shows, in one response.
 *
 * @param id             tournament id
 * @param locke          randomlocke it belongs to
 * @param game           game it is played in
 * @param position       order inside the randomlocke
 * @param name           tournament name
 * @param description    description
 * @param format         groups_bracket or round_robin
 * @param status         draft, in_progress or finished
 * @param pointsPerWin   points per series won
 * @param teamsRevealAt  moment the team pictures become visible to everybody, or null
 * @param teamsRevealed  whether that moment has passed
 * @param bracketSize    size of the generated bracket, null when there is none or it was imported
 * @param players        players with their group and team picture
 * @param matches        every series with its games
 * @param standings      final standings (reward pick order)
 * @param groups         group tables
 * @param rewards        rewards and who picked them
 *
 * @author adrianpmdev
 */
public record TournamentDetailResponse(
        UUID id,
        RefResponse locke,
        RefResponse game,
        int position,
        String name,
        String description,
        String format,
        String status,
        int pointsPerWin,
        Instant teamsRevealAt,
        boolean teamsRevealed,
        Integer bracketSize,
        List<TournamentPlayerResponse> players,
        List<MatchResponse> matches,
        List<StandingResponse> standings,
        List<GroupStandingsResponse> groups,
        List<RewardResponse> rewards
) {
}
