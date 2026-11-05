package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;
import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryClient;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Reads characters from the official armory when somebody looks at them. Nothing is stored: a character read is kept in
 * memory for {@code cacheMinutes} of config.json, so looking again does not ask the armory, and two people asking for
 * the same character at once share one read. Several characters are read a few at a time, never more, to go easy on
 * the armory and on the memory of the server. A failed read is not kept, so the next look tries again.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
public class ArmoryReader {

    /** Characters read at the same time. */
    private static final int AT_ONCE = 4;
    /** Longest wait for several characters; what is not read by then shows as failed and stays on its way. */
    private static final Duration WAIT = Duration.ofSeconds(25);
    private static final int MAX_KEPT = 200;

    private final ArmoryClient armoryClient;
    private final Cache<String, Optional<ArmorySheet>> kept;
    private final ExecutorService readers = Executors.newFixedThreadPool(AT_ONCE, Thread.ofPlatform()
            .name("armory-", 1).daemon(true).factory());

    /**
     * What the armory says of a character.
     *
     * @param status FOUND, NOT_FOUND when the armory does not show it (below level 10, or a wrong name), or FAILED
     * @param sheet  the character when found, otherwise null
     */
    public record Result(Status status, ArmorySheet sheet) {
    }

    /** How a read went. */
    public enum Status { FOUND, NOT_FOUND, FAILED }

    /**
     * Builds the reader.
     *
     * @param armoryClient    reads the armory
     * @param gameDataService gives how long a character is kept
     */
    public ArmoryReader(ArmoryClient armoryClient, GameDataService gameDataService) {
        this.armoryClient = armoryClient;
        this.kept = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(gameDataService.getConfig().armory().cacheMinutes()))
                .maximumSize(MAX_KEPT)
                .build();
    }

    /**
     * Reads a character.
     *
     * @param name full name
     * @return what the armory says
     */
    public Result read(String name) {
        try {
            return kept.get(name.toLowerCase(Locale.ROOT), key -> load(name))
                    .map(sheet -> new Result(Status.FOUND, sheet))
                    .orElse(new Result(Status.NOT_FOUND, null));
        } catch (UncheckedIOException exception) {
            log.warn("A character could not be read from the armory: {}", exception.getCause().getMessage());
            return new Result(Status.FAILED, null);
        }
    }

    /**
     * Reads several characters, a few at a time.
     *
     * @param names full names
     * @return what the armory says of each, by name, in the same order
     */
    public Map<String, Result> readAll(List<String> names) {
        Map<String, CompletableFuture<Result>> reads = new LinkedHashMap<>();

        names.forEach(name -> reads.put(name, CompletableFuture.supplyAsync(() -> read(name), readers)));

        long deadline = System.nanoTime() + WAIT.toNanos();
        Map<String, Result> results = new LinkedHashMap<>();

        reads.forEach((name, read) -> results.put(name, waitFor(read, deadline)));

        return results;
    }

    /** Stops the readers with the application. */
    @PreDestroy
    public void stop() {
        readers.shutdownNow();
    }

    private static Result waitFor(CompletableFuture<Result> read, long deadline) {
        try {
            return read.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Result(Status.FAILED, null);
        } catch (Exception exception) {
            return new Result(Status.FAILED, null);
        }
    }

    private Optional<ArmorySheet> load(String name) {
        try {
            return armoryClient.character(name);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
