package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.domain.TreeGrid;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes the {@code legacy} section: the points every character gets, the perk trees and the account challenges that
 * give those points. Challenges are the achievements the client marks as Legacy challenges, grouped by their top
 * achievement category and kept in the game's order.
 *
 * @author adrianpmdev
 */
final class LegacyExporter {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final int NO_PARENT = -1;

    private final ClientTables tables;
    private final NodeWriter nodes;
    private final GameDataConfig.Client settings;

    LegacyExporter(ClientTables tables, ClientSpells spells, GameDataConfig.Client settings) {
        this.tables = tables;
        this.nodes = new NodeWriter(spells);
        this.settings = settings;
    }

    /**
     * The section.
     *
     * @param trees  every trait tree
     * @param source where the data comes from, with the build
     * @return the Legacy System
     */
    ObjectNode export(List<TraitTrees.Tree> trees, String source) {
        ObjectNode section = JSON.objectNode();
        Row currency = tables.get(ClientTable.TRAIT_CURRENCY).indexBy("ID").get(settings.legacyCurrencyId());
        section.put("points", currency == null ? 0 : currency.integer("SourcedMax"));
        section.put("source", source);
        section.set("trees", trees(trees));
        section.set("challenges", challenges());

        return section;
    }

    /** The perk trees: first those named in the settings, in their order, then any other the client adds. */
    private ArrayNode trees(List<TraitTrees.Tree> trees) {
        List<TraitTrees.Tree> legacyTrees = new ArrayList<>(trees.stream()
                .filter(tree -> tree.currencyId() == settings.legacyCurrencyId())
                .toList());
        List<String> order = List.copyOf(settings.legacyTrees().keySet());
        legacyTrees.sort(Comparator.comparingInt((TraitTrees.Tree tree) -> {
            int index = order.indexOf(Integer.toString(tree.id()));

            return index < 0 ? Integer.MAX_VALUE : index;
        }).thenComparingInt(TraitTrees.Tree::id));

        ArrayNode written = JSON.arrayNode();

        for (TraitTrees.Tree tree : legacyTrees) {
            GameDataConfig.LegacyTree named = settings.legacyTrees().get(Integer.toString(tree.id()));
            Map<TraitTrees.Node, TreeGrid.Position> positions = new LinkedHashMap<>();
            tree.nodes().forEach(node -> positions.put(node, node.position()));

            ObjectNode treeNode = written.addObject();
            treeNode.put("id", tree.id());
            treeNode.put("name", named == null ? "" : named.name());
            treeNode.put("icon", named == null ? "" : named.icon());
            treeNode.set("perks", nodes.write(TreeGrid.layout(positions), NodeWriter.Kind.PERK));
        }

        return written;
    }

    private ArrayNode challenges() {
        Map<Integer, Row> categories = tables.get(ClientTable.ACHIEVEMENT_CATEGORY).indexBy("ID");
        List<Row> achievements = new ArrayList<>(tables.get(ClientTable.ACHIEVEMENT).rows().stream()
                .filter(row -> (row.longValue("Flags") & settings.legacyChallengeFlag()) != 0)
                .toList());
        achievements.sort(Comparator
                .comparing((Row row) -> categoryOrder(categories, row.integer("Category")), LegacyExporter::compare)
                .thenComparingInt(row -> row.integer("Ui_order"))
                .thenComparingInt(row -> row.integer("ID")));

        ArrayNode written = JSON.arrayNode();
        Set<String> seen = new HashSet<>();

        for (Row achievement : achievements) {
            String name = achievement.text("Title_lang");

            if (seen.add(name)) {
                ObjectNode challenge = written.addObject();
                challenge.put("group", topCategory(categories, achievement.integer("Category")));
                challenge.put("name", name);
                challenge.put("desc", achievement.text("Description_lang"));
                challenge.put("points", settings.legacyPointsPerChallenge());
            }
        }

        return written;
    }

    /** Name of the top category of an achievement category. */
    private static String topCategory(Map<Integer, Row> categories, int categoryId) {
        Row category = categories.get(categoryId);

        while (category != null && categories.containsKey(category.integer("Parent"))) {
            category = categories.get(category.integer("Parent"));
        }

        return category == null ? "" : category.text("Name_lang");
    }

    /**
     * Where a category sorts: the UI order and id of each category from the top one down to it, so challenges sort as
     * the game lists them.
     */
    private static List<Integer> categoryOrder(Map<Integer, Row> categories, int categoryId) {
        List<Row> path = new ArrayList<>();

        for (Row category = categories.get(categoryId); category != null;
             category = category.integer("Parent") == NO_PARENT ? null : categories.get(category.integer("Parent"))) {
            path.addFirst(category);
        }

        List<Integer> order = new ArrayList<>();
        path.forEach(category -> {
            order.add(category.integer("Ui_order"));
            order.add(category.integer("ID"));
        });

        return order;
    }

    private static int compare(List<Integer> first, List<Integer> second) {
        for (int index = 0; index < Math.min(first.size(), second.size()); index++) {
            int compared = Integer.compare(first.get(index), second.get(index));

            if (compared != 0) {
                return compared;
            }
        }

        return Integer.compare(first.size(), second.size());
    }
}
