package com.adrianpmdev.platform.profile;

import java.util.List;

/**
 * What a game shows on the profile of a player, made of the kinds of pieces every web client knows how to draw. A new
 * game fills them with its own data and needs no change in the clients.
 *
 * @param summary  a few short texts for the line of the game in the overview, such as {@code 63 puntos}
 * @param sections what the tab of the game shows, in order
 *
 * @author adrianpmdev
 */
public record ProfileBlock(List<String> summary, List<Section> sections) {

    /**
     * Copies the lists.
     */
    public ProfileBlock {
        summary = summary == null ? List.of() : List.copyOf(summary);
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    /**
     * A part of the tab of a game. Each list can be empty.
     *
     * @param title section title
     * @param stats figures
     * @param bars  values with a maximum
     * @param cards things of the player
     */
    public record Section(String title, List<Stat> stats, List<Bar> bars, List<Card> cards) {

        /**
         * Copies the lists.
         */
        public Section {
            stats = stats == null ? List.of() : List.copyOf(stats);
            bars = bars == null ? List.of() : List.copyOf(bars);
            cards = cards == null ? List.of() : List.copyOf(cards);
        }
    }

    /**
     * A figure.
     *
     * @param label what it counts
     * @param value its value
     */
    public record Stat(String label, String value) {
    }

    /**
     * A value with a maximum, drawn as a bar.
     *
     * @param label   what it measures
     * @param current its value, or null when it is not known
     * @param max     the largest value
     * @param iconUrl icon, or null
     */
    public record Bar(String label, Integer current, int max, String iconUrl) {
    }

    /**
     * A thing of the player, such as a character.
     *
     * @param title    its name
     * @param subtitle what it is
     * @param iconUrl  icon, or null
     * @param bars     its values
     */
    public record Card(String title, String subtitle, String iconUrl, List<Bar> bars) {

        /**
         * Copies the list.
         */
        public Card {
            bars = bars == null ? List.of() : List.copyOf(bars);
        }
    }
}
