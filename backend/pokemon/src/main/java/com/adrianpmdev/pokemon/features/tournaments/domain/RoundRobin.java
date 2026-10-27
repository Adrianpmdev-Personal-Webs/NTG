package com.adrianpmdev.pokemon.features.tournaments.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Round robin calendar with the circle method: everybody plays everybody once, spread over rounds in
 * which nobody plays twice. With an odd number of players one player rests every round.
 *
 * @author adrianpmdev
 */
public final class RoundRobin {

    /**
     * One series of the calendar.
     *
     * @param round   round number (1-based)
     * @param player1 first player
     * @param player2 second player
     */
    public record Pairing(int round, UUID player1, UUID player2) {
    }

    private RoundRobin() {
    }

    /**
     * Builds the calendar.
     *
     * @param players players in seeding order
     * @return every pairing, ordered by round
     */
    public static List<Pairing> schedule(List<UUID> players) {
        List<UUID> circle = new ArrayList<>(players);

        if (circle.size() % 2 != 0) {
            // A placeholder makes the count even; whoever meets it rests that round.
            circle.add(null);
        }

        int size = circle.size();
        List<Pairing> pairings = new ArrayList<>();

        for (int round = 1; round < size; round++) {
            for (int i = 0; i < size / 2; i++) {
                UUID home = circle.get(i);
                UUID away = circle.get(size - 1 - i);

                if (home != null && away != null) {
                    pairings.add(new Pairing(round, home, away));
                }
            }

            // Keep the first player fixed and rotate the rest one step.
            circle.add(1, circle.remove(size - 1));
        }

        return pairings;
    }
}
