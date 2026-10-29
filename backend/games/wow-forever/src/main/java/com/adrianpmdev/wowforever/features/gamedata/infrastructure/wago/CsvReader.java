package com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Streams the rows of a CSV file (RFC 4180: commas, double quotes, quotes doubled inside quoted fields, line breaks
 * inside quoted fields). It never keeps the whole file, so a large table only costs the rows its caller keeps.
 *
 * @author adrianpmdev
 */
final class CsvReader {

    private static final int BUFFER_SIZE = 64 * 1024;

    private CsvReader() {
    }

    /**
     * Reads every row, the header included, in file order.
     *
     * @param reader source, read until its end
     * @param rows   receives each row as its fields
     * @throws IOException when the source cannot be read or ends inside a quoted field
     */
    static void read(Reader reader, Consumer<String[]> rows) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean quoteSeen = false;
        boolean rowStarted = false;
        int read;

        while ((read = reader.read(buffer)) != -1) {
            for (int index = 0; index < read; index++) {
                char character = buffer[index];

                if (quoted) {
                    if (quoteSeen) {
                        quoteSeen = false;

                        if (character == '"') {
                            field.append('"');
                            continue;
                        }

                        quoted = false;
                    } else if (character == '"') {
                        quoteSeen = true;
                        continue;
                    } else {
                        field.append(character);
                        continue;
                    }
                }

                switch (character) {
                    case '"' -> {
                        quoted = true;
                        rowStarted = true;
                    }
                    case ',' -> {
                        fields.add(field.toString());
                        field.setLength(0);
                        rowStarted = true;
                    }
                    case '\n' -> {
                        if (rowStarted || !field.isEmpty()) {
                            fields.add(field.toString());
                            rows.accept(fields.toArray(String[]::new));
                        }

                        fields.clear();
                        field.setLength(0);
                        rowStarted = false;
                    }
                    case '\r' -> {
                    }
                    default -> {
                        field.append(character);
                        rowStarted = true;
                    }
                }
            }
        }

        if (quoted && !quoteSeen) {
            throw new IOException("The CSV ends inside a quoted field");
        }

        if (rowStarted || !field.isEmpty()) {
            fields.add(field.toString());
            rows.accept(fields.toArray(String[]::new));
        }
    }
}
