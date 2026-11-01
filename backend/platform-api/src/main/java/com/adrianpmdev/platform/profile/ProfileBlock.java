package com.adrianpmdev.platform.profile;

import java.util.List;

/**
 * What a game shows on the profile of a player. Its texts are ready to read, so the web clients only draw them.
 *
 * @param stats   figures of the player, such as the points or the characters
 * @param entries things of the player, such as each character with its details
 *
 * @author adrianpmdev
 */
public record ProfileBlock(List<Stat> stats, List<Entry> entries) {

    /**
     * Validates the block and copies the lists.
     */
    public ProfileBlock {
        stats = stats == null ? List.of() : List.copyOf(stats);
        entries = entries == null ? List.of() : List.copyOf(entries);
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
     * A thing of the player.
     *
     * @param title  its name
     * @param detail what to tell about it
     */
    public record Entry(String title, String detail) {
    }
}
