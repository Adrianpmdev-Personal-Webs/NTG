package com.adrianpmdev.pokemon.features.tournaments.domain;

import com.adrianpmdev.pokemon.shared.error.ApiErrors;

import java.util.Arrays;

/**
 * Life cycle of a tournament. Admins move it by hand.
 *
 * @author adrianpmdev
 */
public enum TournamentStatus {

    DRAFT("draft"),
    IN_PROGRESS("in_progress"),
    FINISHED("finished");

    private final String value;

    TournamentStatus(String value) {
        this.value = value;
    }

    /**
     * Text stored in the database and used by the API.
     *
     * @return status value
     */
    public String value() {
        return value;
    }

    /**
     * Parses an API or database value.
     *
     * @param value text value
     * @return matching status
     */
    public static TournamentStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equals(value))
                .findFirst()
                .orElseThrow(() -> ApiErrors.badRequest("Unknown tournament status: " + value));
    }
}
