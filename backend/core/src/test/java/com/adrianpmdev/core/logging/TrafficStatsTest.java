package com.adrianpmdev.core.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What each answered request adds to the counters.
 *
 * @author adrianpmdev
 */
class TrafficStatsTest {

    private final TrafficStats stats = new TrafficStats();

    @Test
    void itCountsTheAnswersAndTheirAverageTime() {
        stats.record("GET", "/api/v1/games", 200, 10);
        stats.record("GET", "/api/v1/games", 404, 20);
        stats.record("GET", "/api/v1/games", 500, 60);

        TrafficStats.Snapshot snapshot = stats.snapshot();

        assertThat(snapshot.requests()).isEqualTo(3);
        assertThat(snapshot.clientErrors()).isEqualTo(1);
        assertThat(snapshot.serverErrors()).isEqualTo(1);
        assertThat(snapshot.averageMillis()).isEqualTo(30);
    }

    @Test
    void aRefusedSignInIsAFailedLoginButOtherRoutesAreNot() {
        stats.record("POST", "/api/v1/auth/login", 401, 1);
        stats.record("POST", "/api/v1/auth/login", 200, 1);
        stats.record("GET", "/api/v1/games", 401, 1);

        assertThat(stats.snapshot().failedLogins()).isEqualTo(1);
    }

    @Test
    void aRequestOverALimitIsRateLimitedNotAFailedLogin() {
        stats.record("POST", "/api/v1/auth/login", 429, 1);

        assertThat(stats.snapshot().rateLimited()).isEqualTo(1);
        assertThat(stats.snapshot().failedLogins()).isZero();
    }

    @Test
    void withoutRequestsTheAverageIsZero() {
        assertThat(stats.snapshot().averageMillis()).isZero();
    }
}
