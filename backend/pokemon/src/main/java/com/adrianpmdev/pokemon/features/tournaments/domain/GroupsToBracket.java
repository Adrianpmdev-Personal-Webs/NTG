package com.adrianpmdev.pokemon.features.tournaments.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rules of the groups_bracket format: how players are split into the two groups, and how the groups
 * become the first round of the bracket.
 *
 * <ul>
 *     <li>There are always two groups of the same size. With an odd number of players, one player (the
 *     first of the shuffled list) skips the groups and goes straight to the bracket.</li>
 *     <li>Every player goes to the bracket. The first of one group meets the last of the other, the second
 *     the second to last, and so on. The player who skipped the groups gets a series of their own.</li>
 *     <li>The bracket is the smallest power of two that fits those series; empty sides become byes.</li>
 * </ul>
 *
 * @author adrianpmdev
 */
public final class GroupsToBracket {

    /** Fewest players a groups_bracket tournament can have. */
    public static final int MIN_PLAYERS = 4;

    public static final String GROUP_A = "A";
    public static final String GROUP_B = "B";

    /**
     * Result of the draw.
     *
     * @param groupA players of group A
     * @param groupB players of group B
     * @param direct player who skips the groups (empty list or one player)
     */
    public record Draw(List<UUID> groupA, List<UUID> groupB, List<UUID> direct) {
    }

    private GroupsToBracket() {
    }

    /**
     * Splits players into two groups.
     *
     * @param shuffled players in random order
     * @return the two groups and, with an odd count, the player who skips them
     */
    public static Draw draw(List<UUID> shuffled) {
        if (shuffled.size() < MIN_PLAYERS) {
            throw new IllegalArgumentException("A groups and bracket tournament needs at least " + MIN_PLAYERS + " players");
        }

        List<UUID> direct = shuffled.size() % 2 == 0 ? List.of() : List.of(shuffled.getFirst());
        List<UUID> rest = shuffled.subList(direct.size(), shuffled.size());
        int half = rest.size() / 2;

        return new Draw(List.copyOf(rest.subList(0, half)), List.copyOf(rest.subList(half, rest.size())), direct);
    }

    /**
     * First-round series of the bracket.
     *
     * @param groupA group A in standings order
     * @param groupB group B in standings order
     * @param direct players who skipped the groups
     * @return series as [side 1, side 2]; side 2 is null for a player who goes through without playing
     */
    public static List<UUID[]> firstRound(List<UUID> groupA, List<UUID> groupB, List<UUID> direct) {
        if (groupA.size() != groupB.size()) {
            throw new IllegalArgumentException("Both groups must have the same number of players");
        }

        List<UUID[]> series = new ArrayList<>();
        int size = groupA.size();

        for (int i = 0; i < size; i++) {
            series.add(new UUID[]{groupA.get(i), groupB.get(size - 1 - i)});
        }

        direct.forEach(player -> series.add(new UUID[]{player, null}));

        return series;
    }
}
