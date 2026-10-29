package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Source;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * The client tables of one export, each read once from its source the first time it is needed.
 *
 * @author adrianpmdev
 */
final class ClientTables {

    private final Db2Source source;
    private final Map<ClientTable, Db2Table> tables = new EnumMap<>(ClientTable.class);

    ClientTables(Db2Source source) {
        this.source = source;
    }

    /**
     * A table.
     *
     * @param table which one
     * @return the table, with the columns and rows ClientTable keeps
     * @throws UncheckedIOException when it cannot be read: the export cannot go on without it
     */
    Db2Table get(ClientTable table) {
        return tables.computeIfAbsent(table, key -> {
            try {
                return source.table(key.tableName(), key.columns(), key.rows());
            } catch (IOException exception) {
                throw new UncheckedIOException("The client table " + key.tableName() + " could not be read",
                        exception);
            }
        });
    }
}
