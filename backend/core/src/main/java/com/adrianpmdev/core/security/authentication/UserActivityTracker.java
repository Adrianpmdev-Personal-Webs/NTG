package com.adrianpmdev.core.security.authentication;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Remembers when each user was last active, for the admin dashboard. Sessions renew themselves, so the last sign in of
 * Supabase says little about someone who stays signed in; this column says when they really used the site.
 * <p>
 * The token filter calls it on every authenticated request, so at most one write per user per
 * {@value #WRITE_EVERY_MINUTES} minutes reaches the database: that is the precision of the value and all it costs.
 * The column is written with plain SQL and is not part of the {@code User} entity, so a database that still lacks it
 * only loses this value (a warning in the logs) and the accounts keep working. A failed write is not tried again until
 * the period ends.
 *
 * @author adrianpmdev
 */
@Component
public class UserActivityTracker {

    private static final Logger LOG = LoggerFactory.getLogger(UserActivityTracker.class);

    static final long WRITE_EVERY_MINUTES = 60;

    private static final long MAX_USERS = 1_000;
    private static final String UPDATE = "update core.\"user\" set last_seen_at = now() where id = ?";

    private final JdbcTemplate jdbcTemplate;

    private final Cache<UUID, Boolean> recentlyWritten = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(WRITE_EVERY_MINUTES))
            .maximumSize(MAX_USERS)
            .build();

    /**
     * Builds the tracker.
     *
     * @param jdbcTemplate plain JDBC access for the column
     */
    public UserActivityTracker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Notes that a user has just made a request.
     *
     * @param userId user id
     */
    public void touch(UUID userId) {
        if (recentlyWritten.asMap().putIfAbsent(userId, Boolean.TRUE) != null) {
            return;
        }

        try {
            jdbcTemplate.update(UPDATE, userId);
        } catch (RuntimeException exception) {
            LOG.warn("The last activity of a user could not be saved: {}", exception.getMessage());
        }
    }
}
