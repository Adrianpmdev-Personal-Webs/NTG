package com.adrianpmdev.wowforever.features.news.application.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * A copy in memory of what another site publishes, read only when someone asks for it and the copy is older than the
 * period given, so the site is read at most once per period however many players ask, and never while nobody does.
 * Requests that arrive while it is being read wait for that same read. When a read fails, the last good copy keeps
 * being served and the site is not asked again until the period ends.
 *
 * @author adrianpmdev
 */
@Slf4j
final class OnDemandCopy {

    private final String name;
    private final Clock clock;

    private volatile Snapshot snapshot;

    /** The last read: the copy (null while no read has worked) and when the site was asked. */
    private record Snapshot(String json, Instant readAt) {
    }

    /** Reads the site and returns what is served. */
    @FunctionalInterface
    interface Source {

        String read() throws IOException;
    }

    /**
     * An empty copy.
     *
     * @param name  what is read, for the logs
     * @param clock clock that dates the reads
     */
    OnDemandCopy(String name, Clock clock) {
        this.name = name;
        this.clock = clock;
    }

    /**
     * The copy, read again first when it is older than {@code refresh}.
     *
     * @param refresh how long a read is kept
     * @param source  reads the site
     * @return the last good read, or null while no read has worked
     */
    String get(Duration refresh, Source source) {
        Snapshot current = snapshot;

        if (current == null || isStale(current, refresh)) {
            synchronized (this) {
                current = snapshot;

                if (current == null || isStale(current, refresh)) {
                    current = read(source, current);
                    snapshot = current;
                }
            }
        }

        return current.json();
    }

    private boolean isStale(Snapshot current, Duration refresh) {
        return !current.readAt().plus(refresh).isAfter(clock.instant());
    }

    private Snapshot read(Source source, Snapshot previous) {
        Instant now = clock.instant();

        try {
            return new Snapshot(source.read(), now);
        } catch (IOException | RuntimeException exception) {
            log.warn("{} could not be read: {}", name, exception.getMessage());

            return new Snapshot(previous == null ? null : previous.json(), now);
        }
    }
}
