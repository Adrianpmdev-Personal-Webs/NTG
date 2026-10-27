package com.adrianpmdev.pokemon.features.tournaments.api.dto.response;

import java.util.UUID;

/**
 * A player inside a tournament.
 *
 * @param id               tournament player id
 * @param playerId         player id
 * @param name             player name
 * @param group            A, B or null
 * @param pointsAdjustment points added to the series wins
 * @param hasTeamImage     whether a team picture was uploaded (even if the caller cannot see it yet)
 * @param teamImageUrl     temporary URL of the team picture, null when there is none or it is hidden
 * @param mine             whether this is the caller
 * @param canEditTeam      whether the caller may upload or remove this team picture now
 *
 * @author adrianpmdev
 */
public record TournamentPlayerResponse(
        UUID id,
        UUID playerId,
        String name,
        String group,
        int pointsAdjustment,
        boolean hasTeamImage,
        String teamImageUrl,
        boolean mine,
        boolean canEditTeam
) {
}
