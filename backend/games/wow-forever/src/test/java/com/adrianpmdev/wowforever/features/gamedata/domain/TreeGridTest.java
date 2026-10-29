package com.adrianpmdev.wowforever.features.gamedata.domain;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rows and columns from the screen positions of the client.
 *
 * @author adrianpmdev
 */
class TreeGridTest {

    @Test
    void splitsTreesSideBySideAndNumbersFromOne() {
        Map<String, TreeGrid.Position> positions = new LinkedHashMap<>();
        positions.put("arms-1", new TreeGrid.Position(1020, 2130));
        positions.put("arms-2", new TreeGrid.Position(1620, 2730));
        positions.put("arms-3", new TreeGrid.Position(2220, 2130));
        positions.put("arms-4", new TreeGrid.Position(2820, 2730));
        positions.put("fury-1", new TreeGrid.Position(5020, 2130));
        positions.put("fury-off", new TreeGrid.Position(5030, 3330));
        positions.put("fury-2", new TreeGrid.Position(5620, 2730));

        Map<String, TreeGrid.Cell> cells = TreeGrid.layout(positions);

        assertThat(cells.get("arms-1")).isEqualTo(new TreeGrid.Cell(0, 1, 1));
        assertThat(cells.get("arms-2")).isEqualTo(new TreeGrid.Cell(0, 2, 2));
        assertThat(cells.get("arms-4")).isEqualTo(new TreeGrid.Cell(0, 2, 4));
        assertThat(cells.get("fury-1")).isEqualTo(new TreeGrid.Cell(1, 1, 1));
        assertThat(cells.get("fury-off")).isEqualTo(new TreeGrid.Cell(1, 3, 1));
        assertThat(cells.get("fury-2")).isEqualTo(new TreeGrid.Cell(1, 2, 2));
    }

    @Test
    void leavesOutNodesParkedFarBelow() {
        Map<String, TreeGrid.Position> positions = new LinkedHashMap<>();
        positions.put("top", new TreeGrid.Position(100, 100));
        positions.put("next", new TreeGrid.Position(700, 700));
        positions.put("parked", new TreeGrid.Position(700, 39300));

        assertThat(TreeGrid.layout(positions)).containsOnlyKeys("top", "next");
    }
}
