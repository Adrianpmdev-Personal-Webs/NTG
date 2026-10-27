package com.adrianpmdev.pokemon.features.tournaments.domain;

import com.adrianpmdev.platform.error.ApiErrors;

import java.util.Arrays;

/**
 * How a tournament is played.
 *
 * @author adrianpmdev
 */
public enum TournamentFormat {

    /** Two round robin groups, then every player goes to a double-elimination bracket. */
    GROUPS_BRACKET("groups_bracket"),

    /** Everybody plays everybody once. */
    ROUND_ROBIN("round_robin");

    private final String value;

    TournamentFormat(String value) {
        this.value = value;
    }

    /**
     * Text stored in the database and used by the API.
     *
     * @return format value
     */
    public String value() {
        return value;
    }

    /**
     * Parses an API or database value.
     *
     * @param value text value
     * @return matching format
     */
    public static TournamentFormat fromValue(String value) {
        return Arrays.stream(values())
                .filter(format -> format.value.equals(value))
                .findFirst()
                .orElseThrow(() -> ApiErrors.badRequest("Unknown tournament format: " + value));
    }
}
