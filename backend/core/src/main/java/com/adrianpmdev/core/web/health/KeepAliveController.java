package com.adrianpmdev.core.web.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Endpoint an external cron calls every few minutes so the free server never goes to sleep.
 * <p>
 * Supabase also pauses a free project after a week without database activity, and these calls alone never touch the
 * database. So at most once a day ({@link #DATABASE_PING_INTERVAL}) a call also runs {@code select 1}: one tiny query
 * a day keeps the project awake without costing anything. A failed query is logged and tried again on the next call;
 * the endpoint still answers, because the server itself is alive.
 *
 * @author adrianpmdev
 */
@Slf4j
@RestController
public class KeepAliveController {

    static final Duration DATABASE_PING_INTERVAL = Duration.ofHours(24);

    private final JdbcTemplate jdbcTemplate;

    /** Last successful database ping; the epoch means "never", so the first call after a start pings. */
    private final AtomicReference<Instant> lastDatabasePing = new AtomicReference<>(Instant.EPOCH);

    /**
     * Builds the controller.
     *
     * @param jdbcTemplate plain JDBC access for the daily ping
     */
    public KeepAliveController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Keeps the service awake and, once a day, the database too.
     *
     * @return a short text
     */
    @GetMapping("/keep-alive")
    public ResponseEntity<String> keepAlive() {
        pingDatabaseIfDue();

        return ResponseEntity.ok("Service is alive");
    }

    private void pingDatabaseIfDue() {
        Instant last = lastDatabasePing.get();
        Instant now = Instant.now();

        // Only the call that wins the update pings, so two calls at once never query twice.
        if (now.isBefore(last.plus(DATABASE_PING_INTERVAL)) || !lastDatabasePing.compareAndSet(last, now)) {
            return;
        }

        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            log.info("Daily database ping done");
        } catch (RuntimeException exception) {
            lastDatabasePing.set(last);
            log.warn("Daily database ping failed: {}", exception.getMessage());
        }
    }
}
