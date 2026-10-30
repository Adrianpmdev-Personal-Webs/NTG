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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Keeps the last good game data file in the private bucket {@code wow-forever-data} (see
 * {@code sql/wow-forever.sql}), compressed with gzip, so a restart of the backend serves it at once instead of
 * building it again from the game client.
 * <p>
 * It also keeps a small mark while a build is being read ({@value #ATTEMPT}), removed when the read ends, whether it
 * worked or not. A mark found at start means that build stopped the backend while it was being read (it ran out of
 * memory), so the sync service can avoid reading it again on every restart.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class GameDataStore {

    static final String BUCKET = "wow-forever-data";
    static final String OBJECT = "data.json.gz";
    static final String ATTEMPT = "reading.json.gz";
    private static final String CONTENT_TYPE = "application/gzip";

    private final ObjectStorage storage;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * A build that was being read.
     *
     * @param build     version of the build
     * @param startedAt when its read started
     */
    public record Attempt(String build, Instant startedAt) {
    }

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

    /**
     * The build whose read never ended, if any.
     *
     * @return the unfinished read, or empty when there is none (or the mark cannot be understood)
     */
    public Optional<Attempt> unfinishedRead() {
        return storage.download(BUCKET, ATTEMPT).map(this::read).flatMap(mark -> {
            try {
                return Optional.of(new Attempt(mark.path("build").asText(),
                        Instant.parse(mark.path("startedAt").asText())));
            } catch (DateTimeParseException exception) {
                return Optional.empty();
            }
        });
    }

    /**
     * Marks the start of the read of a build.
     *
     * @param attempt the build and when its read starts
     */
    public void startRead(Attempt attempt) {
        JsonNode mark = objectMapper.createObjectNode()
                .put("build", attempt.build())
                .put("startedAt", attempt.startedAt().toString());
        storage.upload(BUCKET, ATTEMPT, write(mark), CONTENT_TYPE);
    }

    /** Removes the mark: the read has ended. */
    public void endRead() {
        storage.delete(BUCKET, List.of(ATTEMPT));
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
