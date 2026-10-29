package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.domain.TreeGrid;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Writes the {@code talents} section: per class (in the client's class order), its icon, the build it was read from
 * and its three trees with every talent (name, ranks, row and column from 1, icon, text of each rank, prerequisite and,
 * for active talents, the cost line).
 * <p>
 * The client keeps the three trees of a class in one trait tree, side by side; the class of a trait tree is the class
 * most of its spells belong to, and when two trees belong to one class the one with more nodes wins. Each tree takes
 * the name, background and icon of the class's talent tab in the same position; a stray group of nodes beyond the
 * class's tabs is left out.
 *
 * @author adrianpmdev
 */
final class TalentsExporter {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private final ClientTables tables;
    private final ClientSpells spells;
    private final NodeWriter nodes;

    TalentsExporter(ClientTables tables, ClientSpells spells) {
        this.tables = tables;
        this.spells = spells;
        this.nodes = new NodeWriter(spells);
    }

    /**
     * The section.
     *
     * @param trees      every trait tree
     * @param currencyId currency of the talent trees
     * @param source     where the data comes from, with the build (the credit line reads the build from it)
     * @return talents by class name
     */
    ObjectNode export(List<TraitTrees.Tree> trees, int currencyId, String source) {
        Map<Integer, Row> classesBySet = tables.get(ClientTable.CHR_CLASSES).rows().stream()
                .collect(Collectors.toMap(row -> row.integer("SpellClassSet"), Function.identity(), (a, b) -> a));
        Map<Integer, TraitTrees.Tree> treeByClass = new HashMap<>();

        for (TraitTrees.Tree tree : trees) {
            Row gameClass = tree.currencyId() == currencyId ? classesBySet.get(classSetOf(tree)) : null;

            if (gameClass != null) {
                treeByClass.merge(gameClass.integer("ID"), tree,
                        (kept, other) -> other.nodes().size() > kept.nodes().size() ? other : kept);
            }
        }

        List<Row> classes = new ArrayList<>(tables.get(ClientTable.CHR_CLASSES).rows());
        classes.sort(Comparator.comparingInt(row -> row.integer("ID")));
        ObjectNode section = JSON.objectNode();

        for (Row gameClass : classes) {
            TraitTrees.Tree tree = treeByClass.get(gameClass.integer("ID"));

            if (tree != null) {
                ObjectNode entry = section.putObject(gameClass.text("Name_lang"));
                entry.put("icon", spells.iconFile(gameClass.integer("IconFileDataID")));
                entry.put("source", source);
                entry.set("trees", trees(gameClass.integer("ID"), tree));
            }
        }

        return section;
    }

    /** The class set most spells of the tree belong to, or 0. */
    private int classSetOf(TraitTrees.Tree tree) {
        Map<Integer, Integer> votes = new HashMap<>();

        for (TraitTrees.Node node : tree.nodes()) {
            int classSet = spells.classSet(node.first().spellId());

            if (classSet > 0) {
                votes.merge(classSet, 1, Integer::sum);
            }
        }

        return votes.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0);
    }

    private ArrayNode trees(int classId, TraitTrees.Tree tree) {
        List<Row> tabs = tables.get(ClientTable.TALENT_TAB).rows().stream()
                .filter(tab -> tab.integer("ClassMask") == ClassSkillLines.classBit(classId))
                .sorted(Comparator.comparingInt(tab -> tab.integer("OrderIndex")))
                .toList();
        Map<TraitTrees.Node, TreeGrid.Position> positions = new LinkedHashMap<>();
        tree.nodes().forEach(node -> positions.put(node, node.position()));
        Map<TraitTrees.Node, TreeGrid.Cell> cells = TreeGrid.layout(positions);
        int laidOut = cells.values().stream().mapToInt(TreeGrid.Cell::tree).max().orElse(-1) + 1;
        int treeCount = Math.min(tabs.size(), laidOut);
        ArrayNode trees = JSON.arrayNode();

        for (int index = 0; index < treeCount; index++) {
            int treeIndex = index;
            Map<TraitTrees.Node, TreeGrid.Cell> treeCells = new LinkedHashMap<>();
            cells.forEach((node, cell) -> {
                if (cell.tree() == treeIndex) {
                    treeCells.put(node, cell);
                }
            });

            Row tab = tabs.get(index);
            ObjectNode treeNode = trees.addObject();
            treeNode.put("name", tab.text("Name_lang"));
            treeNode.put("bg", tab.integer("ID"));
            treeNode.put("icon", spells.iconFile(tab.integer("SpellIconID")));
            treeNode.set("talents", nodes.write(treeCells, NodeWriter.Kind.TALENT));
        }

        return trees;
    }
}
