package com.adrianpmdev.wowforever.features.gamedata.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Turns the positions the game client gives the nodes of a tree (screen coordinates) into rows and columns. A talent
 * tree of the client holds the three trees of a class side by side; they are told apart by the wide gaps between
 * them, so each one gets its own columns. Nodes the client parks far below the trees (unused slots) are left out.
 *
 * @author adrianpmdev
 */
public final class TreeGrid {

    /** A gap wider than this many columns separates two trees. */
    private static final int TREE_GAP_IN_COLUMNS = 3;
    /** No tree of the game is this tall: a node further down is a parked slot. */
    private static final int MAX_ROWS = 15;

    private TreeGrid() {
    }

    /**
     * A position of the client.
     *
     * @param x horizontal coordinate
     * @param y vertical coordinate, growing downwards
     */
    public record Position(int x, int y) {
    }

    /**
     * Where a node goes.
     *
     * @param tree   tree, from 0 (left to right)
     * @param row    row, from 1 (top first)
     * @param column column inside its tree, from 1
     */
    public record Cell(int tree, int row, int column) {
    }

    /**
     * Lays out the nodes.
     *
     * @param positions position of each node
     * @param <K>       node key
     * @return the cell of each node, in the order of the positions (parked nodes left out)
     */
    public static <K> Map<K, Cell> layout(Map<K, Position> positions) {
        Map<K, Cell> cells = new LinkedHashMap<>();

        if (positions.isEmpty()) {
            return cells;
        }

        TreeSet<Integer> xs = new TreeSet<>();
        TreeSet<Integer> ys = new TreeSet<>();
        positions.values().forEach(position -> {
            xs.add(position.x());
            ys.add(position.y());
        });

        int columnStep = step(xs);
        int rowStep = step(ys);
        List<Integer> treeStarts = treeStarts(xs, columnStep);
        int top = ys.first();

        positions.forEach((key, position) -> {
            int tree = treeOf(treeStarts, position.x());
            int column = Math.round((float) (position.x() - treeStarts.get(tree)) / columnStep) + 1;
            int row = Math.round((float) (position.y() - top) / rowStep) + 1;

            if (row <= MAX_ROWS) {
                cells.put(key, new Cell(tree, row, column));
            }
        });

        return cells;
    }

    /**
     * The distance between neighbouring rows or columns: the most common gap between two neighbouring values (the
     * smaller one on a tie), so a node placed a few units off its column does not count as a column of its own. 1 when
     * there is only one value.
     */
    private static int step(TreeSet<Integer> values) {
        Map<Integer, Integer> gaps = new TreeMap<>();
        Integer previous = null;

        for (int value : values) {
            if (previous != null) {
                gaps.merge(value - previous, 1, Integer::sum);
            }

            previous = value;
        }

        return gaps.entrySet().stream()
                .max(Map.Entry.<Integer, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey)
                .orElse(1);
    }

    private static List<Integer> treeStarts(Collection<Integer> xs, int columnStep) {
        List<Integer> starts = new ArrayList<>();
        Integer previous = null;

        for (int x : xs) {
            if (previous == null || x - previous > columnStep * TREE_GAP_IN_COLUMNS) {
                starts.add(x);
            }

            previous = x;
        }

        return starts;
    }

    private static int treeOf(List<Integer> treeStarts, int x) {
        int tree = 0;

        while (tree + 1 < treeStarts.size() && treeStarts.get(tree + 1) <= x) {
            tree++;
        }

        return tree;
    }
}
