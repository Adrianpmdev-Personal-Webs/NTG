package com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage;

import com.adrianpmdev.platform.port.ObjectStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Keeps in the private bucket {@code wow-forever-data} (see {@code sql/wow-forever.sql}), compressed with gzip:
 * <ul>
 *     <li>{@value #DATA}: the last good game data file, so a restart serves it at once instead of reading the game
 *     client again;</li>
 *     <li>{@value #CLASSIC}: the Classic reference, read once from the client of Classic Era, which does not
 *     change;</li>
 *     <li>{@value #PENDING}: the build waiting to be read on a clean start, and how many reads of it were started and
 *     never ended well. It is removed once that build is served and kept.</li>
 * </ul>
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class GameDataStore {

    static final String BUCKET = "wow-forever-data";
    static final String DATA = "data.json.gz";
    static final String CLASSIC = "classic.json.gz";
    static final String PENDING = "pending.json.gz";
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
        return storage.download(BUCKET, DATA).map(this::read);
    }

    /**
     * Keeps a data file, replacing the previous one.
     *
     * @param data the file
     */
    public void save(JsonNode data) {
        storage.upload(BUCKET, DATA, write(data), CONTENT_TYPE);
    }

    /**
     * The Classic reference kept last time.
     *
     * @return the reference, or empty when none has been kept yet
     * @throws UncheckedIOException when the kept reference cannot be read
     */
    public Optional<JsonNode> loadClassic() {
        return storage.download(BUCKET, CLASSIC).map(this::read);
    }

    /**
     * Keeps the Classic reference, replacing the previous one.
     *
     * @param reference the reference
     */
    public void saveClassic(JsonNode reference) {
        storage.upload(BUCKET, CLASSIC, write(reference), CONTENT_TYPE);
    }

    /**
     * The build waiting to be read.
     *
     * @return the pending build, or empty when there is none (or its note cannot be understood)
     */
    public Optional<PendingBuild> pending() {
        return storage.download(BUCKET, PENDING).map(this::read).flatMap(note -> {
            String build = note.path("build").asText("");

            return build.isEmpty() ? Optional.empty() : Optional.of(new PendingBuild(build,
                    note.path("exporter").asText(""), note.path("deploy").asText(""),
                    note.path("attempts").asInt(), note.path("lastError").asText(null)));
        });
    }

    /**
     * Notes the build waiting to be read, replacing the previous note.
     *
     * @param pending the build and its attempts
     */
    public void savePending(PendingBuild pending) {
        ObjectNode note = objectMapper.createObjectNode()
                .put("build", pending.build())
                .put("exporter", pending.exporter())
                .put("deploy", pending.deploy())
                .put("attempts", pending.attempts())
                .put("lastError", pending.lastError());
        storage.upload(BUCKET, PENDING, write(note), CONTENT_TYPE);
    }

    /** Removes the note of the pending build: it is served and kept. */
    public void deletePending() {
        storage.delete(BUCKET, List.of(PENDING));
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
