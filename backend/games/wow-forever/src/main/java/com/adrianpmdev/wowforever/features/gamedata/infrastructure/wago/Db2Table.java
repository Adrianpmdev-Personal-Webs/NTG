package com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * One table of the game client (a DB2 file) as wago.tools exports it, keeping only the columns its reader asked for:
 * the client tables are large, and most of their columns are never used. A column asked for as {@code ?Name} is
 * optional (one game version has it and another does not): when the export lacks it, it reads as empty.
 *
 * @author adrianpmdev
 */
public final class Db2Table {

    /** Marks a column that an export may lack. */
    public static final String OPTIONAL = "?";

    private final String name;
    private final Map<String, Integer> columns;
    private final List<Row> rows;

    private Db2Table(String name, Map<String, Integer> columns, List<Row> rows) {
        this.name = name;
        this.columns = columns;
        this.rows = rows;
    }

    /**
     * Reads a table from its CSV export.
     *
     * @param name    table name, for error messages
     * @param csv     the export, header first
     * @param columns columns to keep
     * @return the table
     * @throws IOException when the export cannot be read or lacks one of the columns
     */
    public static Db2Table read(String name, Reader csv, Collection<String> columns) throws IOException {
        return read(name, csv, columns, row -> true);
    }

    /**
     * Reads a table from its CSV export, keeping only some of its rows.
     *
     * @param name    table name, for error messages
     * @param csv     the export, header first
     * @param columns columns to keep
     * @param keep    rows to keep
     * @return the table
     * @throws IOException when the export cannot be read or lacks one of the columns
     */
    public static Db2Table read(String name, Reader csv, Collection<String> columns, Predicate<Row> keep)
            throws IOException {
        List<String> wanted = columns.stream().map(Db2Table::columnName).toList();
        List<Boolean> optional = columns.stream().map(column -> column.startsWith(OPTIONAL)).toList();
        int[][] positions = new int[1][];
        List<Row> rows = new ArrayList<>();
        Map<String, Integer> indexes = new LinkedHashMap<>();
        Db2Table table = new Db2Table(name, Collections.unmodifiableMap(indexes), Collections.unmodifiableList(rows));

        CsvReader.read(csv, fields -> {
            if (positions[0] == null) {
                positions[0] = positionsOf(fields, wanted);
                wanted.forEach(column -> indexes.put(column, indexes.size()));
                return;
            }

            String[] values = new String[positions[0].length];

            for (int index = 0; index < values.length; index++) {
                int position = positions[0][index];
                values[index] = position >= 0 && position < fields.length ? fields[position] : "";
            }

            Row row = table.new Row(values);

            if (keep.test(row)) {
                rows.add(row);
            }
        });

        if (positions[0] == null) {
            throw new IOException("The table " + name + " is empty");
        }

        for (int index = 0; index < wanted.size(); index++) {
            if (positions[0][index] < 0 && !optional.get(index)) {
                throw new IOException("The table " + name + " has no column " + wanted.get(index));
            }
        }

        return table;
    }

    private static String columnName(String column) {
        return column.startsWith(OPTIONAL) ? column.substring(OPTIONAL.length()) : column;
    }

    private static int[] positionsOf(String[] header, List<String> wanted) {
        Map<String, Integer> byName = new HashMap<>();

        for (int index = 0; index < header.length; index++) {
            byName.put(header[index], index);
        }

        return wanted.stream().mapToInt(column -> byName.getOrDefault(column, -1)).toArray();
    }

    /** Table name. */
    public String name() {
        return name;
    }

    /** Every row, in file order. */
    public List<Row> rows() {
        return rows;
    }

    /**
     * The rows by the value of one column; the first row wins when the value repeats.
     *
     * @param column integer column, usually {@code ID}
     * @return rows by that value
     */
    public Map<Integer, Row> indexBy(String column) {
        Map<Integer, Row> index = new HashMap<>();
        rows.forEach(row -> index.putIfAbsent(row.integer(column), row));

        return index;
    }

    /**
     * The rows grouped by the value of one column, in file order inside each group.
     *
     * @param column integer column, for example {@code SpellID}
     * @return rows by that value
     */
    public Map<Integer, List<Row>> groupBy(String column) {
        Map<Integer, List<Row>> groups = new HashMap<>();
        rows.forEach(row -> groups.computeIfAbsent(row.integer(column), key -> new ArrayList<>()).add(row));

        return groups;
    }

    /** One row: its values are read by column name. */
    public final class Row {

        private final String[] values;

        private Row(String[] values) {
            this.values = values;
        }

        /** The value as written, or an empty text. */
        public String text(String column) {
            return values[indexOf(column)];
        }

        /** The value as an integer; empty or not a number is 0. */
        public int integer(String column) {
            return (int) longValue(column);
        }

        /** The value as a long integer (race masks need 64 bits); empty or not a number is 0. */
        public long longValue(String column) {
            String value = text(column);

            try {
                return value.isEmpty() ? 0 : Long.parseLong(value);
            } catch (NumberFormatException exception) {
                return (long) decimal(column);
            }
        }

        /** The value as a decimal number; empty or not a number is 0. */
        public double decimal(String column) {
            String value = text(column);

            try {
                return value.isEmpty() ? 0 : Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                return 0;
            }
        }

        private int indexOf(String column) {
            Integer index = columns.get(column);

            if (index == null) {
                throw new IllegalArgumentException("The column " + column + " of " + name + " was not read");
            }

            return index;
        }
    }
}
