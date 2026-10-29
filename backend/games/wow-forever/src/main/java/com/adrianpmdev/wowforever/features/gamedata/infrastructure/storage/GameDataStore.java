package com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage;

import com.adrianpmdev.platform.port.ObjectStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Keeps the last good game data file in the private bucket {@code wow-forever-data} (see
 * {@code sql/wow-forever.sql}), compressed with gzip, so a restart of the backend serves it at once instead of
 * building it again from the game client.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class GameDataStore {

    static final String BUCKET = "wow-forever-data";
    static final String OBJECT = "data.json.gz";
    private static final String CONTENT_TYPE = "application/gzip";

    private final ObjectStorage storage;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The data file kept last time.
     *
     * @return the file, or empty when none has been kept yet
     * @throws UncheckedIOException when the kept file cannot be read
     */
    public Optional<JsonNode> load() {
        return storage.download(BUCKET, OBJECT).map(this::read);
    }

    /**
     * Keeps a data file, replacing the previous one.
     *
     * @param data the file
     */
    public void save(JsonNode data) {
        storage.upload(BUCKET, OBJECT, write(data), CONTENT_TYPE);
    }

    private JsonNode read(byte[] compressed) {
        try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return objectMapper.readTree(input);
        } catch (IOException exception) {
            throw new UncheckedIOException("The kept game data cannot be read", exception);
        }
    }

    private byte[] write(JsonNode data) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (OutputStream output = new GZIPOutputStream(bytes)) {
            objectMapper.writeValue(output, data);
        } catch (IOException exception) {
            throw new UncheckedIOException("The game data cannot be written", exception);
        }

        return bytes.toByteArray();
    }
}
