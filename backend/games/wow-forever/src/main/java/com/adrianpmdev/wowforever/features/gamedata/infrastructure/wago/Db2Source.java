package com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Where the tables of one build of the game client are read from: wago.tools in production, a folder of CSV exports
 * in tests.
 *
 * @author adrianpmdev
 */
@FunctionalInterface
public interface Db2Source {

    /**
     * Reads one table, keeping only some of its rows.
     *
     * @param name    table name, as wago.tools names it (for example {@code SpellName})
     * @param columns columns to keep
     * @param keep    rows to keep
     * @return the table
     * @throws IOException when the table cannot be read or lacks one of the columns
     */
    Db2Table table(String name, Collection<String> columns, Predicate<Db2Table.Row> keep) throws IOException;

    /**
     * Reads one whole table.
     *
     * @param name    table name
     * @param columns columns to keep
     * @return the table
     * @throws IOException when the table cannot be read or lacks one of the columns
     */
    default Db2Table table(String name, Collection<String> columns) throws IOException {
        return table(name, columns, row -> true);
    }
}
