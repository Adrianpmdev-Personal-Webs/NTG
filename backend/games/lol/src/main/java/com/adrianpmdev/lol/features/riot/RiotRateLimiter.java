package com.adrianpmdev.lol.features.riot;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Keeps the calls under the limits of the key (100 every 2 minutes, 20 per second), with a margin. Downloads in the
 * background stop at {@value #BACKGROUND_LIMIT}, so what a player is waiting for always finds room up to
 * {@value #INTERACTIVE_LIMIT}. After a 429 nobody calls until Riot says so.
 *
 * @author adrianpmdev
 */
@Component
public class RiotRateLimiter {

    static final int BACKGROUND_LIMIT = 80;
    static final int INTERACTIVE_LIMIT = 95;

    private static final long WINDOW_MILLIS = 120_000;
    private static final long MIN_GAP_MILLIS = 70;

    private final Deque<Long> calls = new ArrayDeque<>();
    private long lastCallAt;
    private long pausedUntil;

    /**
     * Waits for room for one call.
     *
     * @param interactive whether a player is waiting for it
     * @throws InterruptedException when the application stops
     */
    public void acquire(boolean interactive) throws InterruptedException {
        while (true) {
            long wait = tryAcquire(System.currentTimeMillis(), interactive ? INTERACTIVE_LIMIT : BACKGROUND_LIMIT);

            if (wait <= 0) {
                return;
            }

            Thread.sleep(wait);
        }
    }

    /**
     * Stops every call for a while.
     *
     * @param seconds what the Retry-After header of a 429 says
     */
    public synchronized void pause(long seconds) {
        pausedUntil = Math.max(pausedUntil, System.currentTimeMillis() + seconds * 1000);
    }

    /**
     * Takes a place if there is room.
     *
     * @param now   current time
     * @param limit calls allowed in the window
     * @return 0 when the call can go, or the milliseconds to wait
     */
    synchronized long tryAcquire(long now, int limit) {
        while (!calls.isEmpty() && now - calls.peekFirst() >= WINDOW_MILLIS) {
            calls.pollFirst();
        }

        if (pausedUntil > now) {
            return pausedUntil - now;
        }

        if (now - lastCallAt < MIN_GAP_MILLIS) {
            return MIN_GAP_MILLIS - (now - lastCallAt);
        }

        if (calls.size() >= limit) {
            return WINDOW_MILLIS - (now - calls.peekFirst()) + 1;
        }

        calls.addLast(now);
        lastCallAt = now;

        return 0;
    }
}
