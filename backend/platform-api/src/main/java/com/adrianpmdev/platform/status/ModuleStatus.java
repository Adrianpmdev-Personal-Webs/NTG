package com.adrianpmdev.platform.status;

import java.util.List;

/**
 * What a module wants the admin dashboard to show about its own state: a title and some lines, already written for
 * the admin, so the platform shows them without knowing what they mean.
 *
 * @param title name of the block
 * @param lines lines of the block, in the order they are shown
 *
 * @author adrianpmdev
 */
public record ModuleStatus(String title, List<Line> lines) {

    /**
     * Copies the lines.
     */
    public ModuleStatus {
        lines = List.copyOf(lines);
    }

    /**
     * How a line is highlighted.
     */
    public enum Level {
        OK, WARNING, ERROR
    }

    /**
     * One line of the block.
     *
     * @param label what the line is about
     * @param value its current value
     * @param level how it is highlighted
     * @param icon  name of the icon the web client shows next to it
     */
    public record Line(String label, String value, Level level, String icon) {
    }
}
