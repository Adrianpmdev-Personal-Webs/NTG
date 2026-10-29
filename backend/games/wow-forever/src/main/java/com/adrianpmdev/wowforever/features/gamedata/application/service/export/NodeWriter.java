package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.domain.TreeGrid;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes the nodes of one tree as the data file lists them: talents ({@code desc} per rank) or Legacy perks
 * ({@code ranks} per rank, the points the tree needs as {@code gate}, and the placeholder slots).
 *
 * @author adrianpmdev
 */
final class NodeWriter {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final Comparator<TreeGrid.Cell> CELL_ORDER = Comparator.comparingInt(TreeGrid.Cell::row)
            .thenComparingInt(TreeGrid.Cell::column);

    /** What the tree holds. */
    enum Kind {
        TALENT, PERK
    }

    private final ClientSpells spells;

    NodeWriter(ClientSpells spells) {
        this.spells = spells;
    }

    /**
     * Writes the nodes, top row first and left to right.
     *
     * @param cells cell of every node of the tree
     * @param kind  talents or perks
     * @return the nodes
     */
    ArrayNode write(Map<TraitTrees.Node, TreeGrid.Cell> cells, Kind kind) {
        List<Map.Entry<TraitTrees.Node, TreeGrid.Cell>> ordered = new ArrayList<>(cells.entrySet());
        ordered.sort(Comparator.comparing(Map.Entry::getValue, CELL_ORDER));
        Map<Integer, String> namesById = new HashMap<>();
        cells.keySet().forEach(node -> namesById.put(node.id(), name(node.first())));
        ArrayNode written = JSON.arrayNode();

        for (Map.Entry<TraitTrees.Node, TreeGrid.Cell> entry : ordered) {
            written.add(node(entry.getKey(), entry.getValue(), kind, namesById));
        }

        return written;
    }

    private ObjectNode node(TraitTrees.Node node, TreeGrid.Cell cell, Kind kind, Map<Integer, String> namesById) {
        TraitTrees.Rank first = node.first();
        int spellId = first.spellId();
        ObjectNode written = JSON.objectNode();
        written.put("name", name(first));
        written.put("max", node.ranks().size());
        written.put("row", cell.row());
        written.put("col", cell.column());

        if (kind == Kind.TALENT) {
            written.put("passive", spells.passive(spellId));
        }

        written.put("icon", first.overrideIconFile() > 0 ? spells.iconFile(first.overrideIconFile())
                : spells.icon(spellId));
        ArrayNode texts = written.putArray(kind == Kind.TALENT ? "desc" : "ranks");
        node.ranks().forEach(rank -> texts.add(spells.description(rank.spellId(), rank.points(),
                rank.overrideDescription())));

        if (kind == Kind.PERK) {
            written.put("gate", node.pointsRequired());
        }

        if (!node.placeholder()) {
            node.requires().stream().map(namesById::get).filter(Objects::nonNull).findFirst()
                    .ifPresent(required -> written.put("req", required));
        }

        String cost = spells.costLine(spellId);

        if (cost != null) {
            written.put("cost", cost);
        }

        if (kind == Kind.PERK && node.placeholder()) {
            written.put("placeholder", true);
        }

        return written;
    }

    private String name(TraitTrees.Rank rank) {
        return rank.overrideName().isEmpty() ? spells.name(rank.spellId()) : rank.overrideName();
    }
}
