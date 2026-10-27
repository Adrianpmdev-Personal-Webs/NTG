package com.adrianpmdev.pokemon.features.tournaments.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Every game of a series, in order. Replaces the games stored before.
 *
 * @param games games played (0 to 3)
 *
 * @author adrianpmdev
 */
public record MatchGamesRequest(@NotNull @Size(max = 3) List<@Valid Game> games) {

    /**
     * One game.
     *
     * @param winnerSide 1 or 2
     * @param replayUrl  replay link (http or https), or null
     */
    public record Game(
            @Min(1) @Max(2) int winnerSide,
            @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "must be an http(s) URL") String replayUrl
    ) {
    }
}
