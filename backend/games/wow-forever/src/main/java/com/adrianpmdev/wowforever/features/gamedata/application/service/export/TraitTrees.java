package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.domain.TreeGrid;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The trees of the game client's trait system, where WoW Forever keeps both the talent trees (one per class, with its
 * three trees side by side) and the Legacy System perk trees. Each tree spends one currency: talent points or Legacy
 * points.
 *
 * @author adrianpmdev
 */
final class TraitTrees {

    /** The node is a slot for a perk still to come. */
    private static final int PLACEHOLDER_FLAG = 0x2;
    /** The rank values of an effect are set (not added or multiplied). */
    private static final int SET_POINTS = 0;

    private TraitTrees() {
    }

    /**
     * One tree.
     *
     * @param id         client id
     * @param currencyId currency its points are spent from
     * @param nodes      nodes, in client id order
     */
    record Tree(int id, int currencyId, List<Node> nodes) {
    }

    /**
     * One node: a talent or a perk.
     *
     * @param id             client id
     * @param position       position on screen
     * @param placeholder    a slot for something still to come
     * @param ranks          what each rank gives, rank 1 first
     * @param pointsRequired points the tree needs spent before this node opens
     * @param requires       ids of the nodes that must be full first
     */
    record Node(int id, TreeGrid.Position position, boolean placeholder, List<Rank> ranks, int pointsRequired,
                List<Integer> requires) {

        /** The first rank, which names the node and gives its icon. */
        Rank first() {
            return ranks.getFirst();
        }
    }

    /**
     * One rank of a node.
     *
     * @param spellId             spell it gives
     * @param points              value of the spell's effects at this rank, by effect index
     * @param overrideName        name shown instead of the spell's, or an empty text
     * @param overrideDescription text shown instead of the spell's, or an empty text
     * @param overrideIconFile    icon file shown instead of the spell's, or 0
     */
    record Rank(int spellId, Map<Integer, Double> points, String overrideName, String overrideDescription,
                int overrideIconFile) {
    }

    /**
     * Reads every tree that spends a currency, in client id order. Nodes without ranks are left out.
     *
     * @param tables client tables
     * @return the trees
     */
    static List<Tree> read(ClientTables tables) {
        Map<Integer, Integer> currencyByTree = new TreeMap<>();
        tables.get(ClientTable.TRAIT_TREE_X_TRAIT_CURRENCY).rows()
                .forEach(row -> currencyByTree.putIfAbsent(row.integer("TraitTreeID"), row.integer("TraitCurrencyID")));

        Map<Integer, List<Row>> nodesByTree = tables.get(ClientTable.TRAIT_NODE).groupBy("TraitTreeID");
        Map<Integer, List<Row>> entryLinks = tables.get(ClientTable.TRAIT_NODE_X_TRAIT_NODE_ENTRY)
                .groupBy("TraitNodeID");
        Map<Integer, Row> entries = tables.get(ClientTable.TRAIT_NODE_ENTRY).indexBy("ID");
        Map<Integer, Row> definitions = tables.get(ClientTable.TRAIT_DEFINITION).indexBy("ID");
        Map<Integer, List<Row>> effectPoints = tables.get(ClientTable.TRAIT_DEFINITION_EFFECT_POINTS)
                .groupBy("TraitDefinitionID");
        Map<Integer, List<Row>> curves = tables.get(ClientTable.CURVE_POINT).groupBy("CurveID");
        Map<Integer, List<Integer>> requiredByNode = new HashMap<>();
        tables.get(ClientTable.TRAIT_EDGE).rows().forEach(edge -> requiredByNode
                .computeIfAbsent(edge.integer("RightTraitNodeID"), key -> new ArrayList<>())
                .add(edge.integer("LeftTraitNodeID")));
        Map<Integer, Integer> pointsRequired = pointsRequiredByNode(tables);

        List<Tree> trees = new ArrayList<>();

        currencyByTree.forEach((treeId, currencyId) -> {
            List<Row> rows = new ArrayList<>(nodesByTree.getOrDefault(treeId, List.of()));
            rows.sort(Comparator.comparingInt(row -> row.integer("ID")));
            List<Node> nodes = new ArrayList<>();

            for (Row node : rows) {
                List<Rank> ranks = ranks(entryLinks.getOrDefault(node.integer("ID"), List.of()), entries,
                        definitions, effectPoints, curves);

                if (!ranks.isEmpty()) {
                    nodes.add(new Node(node.integer("ID"),
                            new TreeGrid.Position(node.integer("PosX"), node.integer("PosY")),
                            (node.integer("Flags") & PLACEHOLDER_FLAG) != 0, ranks,
                            pointsRequired.getOrDefault(node.integer("ID"), 0),
                            List.copyOf(requiredByNode.getOrDefault(node.integer("ID"), List.of()))));
                }
            }

            trees.add(new Tree(treeId, currencyId, List.copyOf(nodes)));
        });

        return List.copyOf(trees);
    }

    /**
     * The ranks of a node: each entry gives as many ranks as its maximum. A node with several entries of one rank
     * each (a tiered node) reads one entry per rank.
     */
    private static List<Rank> ranks(List<Row> links, Map<Integer, Row> entries, Map<Integer, Row> definitions,
                                    Map<Integer, List<Row>> effectPoints, Map<Integer, List<Row>> curves) {
        List<Row> ordered = new ArrayList<>(links);
        ordered.sort(Comparator.comparingInt(row -> row.integer("_Index")));
        List<Rank> ranks = new ArrayList<>();

        for (Row link : ordered) {
            Row entry = entries.get(link.integer("TraitNodeEntryID"));
            Row definition = entry == null ? null : definitions.get(entry.integer("TraitDefinitionID"));

            if (definition == null || definition.integer("SpellID") == 0) {
                continue;
            }

            List<Row> points = effectPoints.getOrDefault(definition.integer("ID"), List.of());

            for (int rank = 1; rank <= Math.max(1, entry.integer("MaxRanks")); rank++) {
                ranks.add(new Rank(definition.integer("SpellID"), pointsAt(points, curves, rank),
                        definition.text("OverrideName_lang"), definition.text("OverrideDescription_lang"),
                        definition.integer("OverrideIcon")));
            }
        }

        return List.copyOf(ranks);
    }

    private static Map<Integer, Double> pointsAt(List<Row> points, Map<Integer, List<Row>> curves, int rank) {
        Map<Integer, Double> values = new LinkedHashMap<>();

        for (Row point : points) {
            List<Row> curve = curves.get(point.integer("CurveID"));

            if (point.integer("OperationType") == SET_POINTS && curve != null) {
                values.put(point.integer("EffectIndex"), curveValue(curve, rank));
            }
        }

        return Map.copyOf(values);
    }

    /** The value of a curve at a point, straight between its two nearest points and flat past its ends. */
    private static double curveValue(List<Row> curve, double at) {
        List<Row> points = new ArrayList<>(curve);
        points.sort(Comparator.comparingDouble(row -> row.decimal("Pos_0")));

        if (at <= points.getFirst().decimal("Pos_0")) {
            return points.getFirst().decimal("Pos_1");
        }

        for (int index = 1; index < points.size(); index++) {
            Row before = points.get(index - 1);
            Row after = points.get(index);

            if (at <= after.decimal("Pos_0")) {
                double span = after.decimal("Pos_0") - before.decimal("Pos_0");
                double share = span == 0 ? 1 : (at - before.decimal("Pos_0")) / span;

                return before.decimal("Pos_1") + share * (after.decimal("Pos_1") - before.decimal("Pos_1"));
            }
        }

        return points.getLast().decimal("Pos_1");
    }

    /** The points a tree needs spent before each node opens: the highest condition of the groups the node is in. */
    private static Map<Integer, Integer> pointsRequiredByNode(ClientTables tables) {
        Map<Integer, Row> conditions = tables.get(ClientTable.TRAIT_COND).indexBy("ID");
        Map<Integer, Integer> requiredByGroup = new HashMap<>();

        for (Row link : tables.get(ClientTable.TRAIT_NODE_GROUP_X_TRAIT_COND).rows()) {
            Row condition = conditions.get(link.integer("TraitCondID"));

            if (condition != null) {
                requiredByGroup.merge(link.integer("TraitNodeGroupID"), condition.integer("SpentAmountRequired"),
                        Math::max);
            }
        }

        Map<Integer, Integer> requiredByNode = new HashMap<>();

        for (Row link : tables.get(ClientTable.TRAIT_NODE_GROUP_X_TRAIT_NODE).rows()) {
            Integer required = requiredByGroup.get(link.integer("TraitNodeGroupID"));

            if (required != null) {
                requiredByNode.merge(link.integer("TraitNodeID"), required, Math::max);
            }
        }

        return requiredByNode;
    }
}
