package com.adrianpmdev.pokemon.features.tournaments.domain;

import com.adrianpmdev.pokemon.shared.error.ApiErrors;

import java.util.Arrays;

/**
 * Where a series is played.
 *
 * @author adrianpmdev
 */
public enum MatchStage {

    /** Group phase of a groups_bracket tournament. */
    GROUP("group"),

    /** Round robin league. */
    LEAGUE("league"),

    /** Winners side of the double-elimination bracket. */
    UPPER("upper"),

    /** Losers side of the double-elimination bracket. */
    LOWER("lower"),

    /** Last series: winner of the upper side against winner of the lower side. */
    GRAND_FINAL("grand_final");

    private final String value;

    MatchStage(String value) {
        this.value = value;
    }

    /**
     * Text stored in the database and used by the API.
     *
     * @return stage value
     */
    public String value() {
        return value;
    }

    /**
     * Whether the stage belongs to the bracket.
     *
     * @return true for upper, lower and grand final
     */
    public boolean isBracket() {
        return this == UPPER || this == LOWER || this == GRAND_FINAL;
    }

    /**
     * Parses an API or database value.
     *
     * @param value text value
     * @return matching stage
     */
    public static MatchStage fromValue(String value) {
        return Arrays.stream(values())
                .filter(stage -> stage.value.equals(value))
                .findFirst()
                .orElseThrow(() -> ApiErrors.badRequest("Unknown match stage: " + value));
    }
}
