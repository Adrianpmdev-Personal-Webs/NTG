package com.adrianpmdev.core.security.authentication;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * How often the last activity reaches the database, and that it never gets in the way of a request.
 *
 * @author adrianpmdev
 */
class UserActivityTrackerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final UserActivityTracker tracker = new UserActivityTracker(jdbcTemplate);

    @Test
    void manyRequestsOfAUserWriteOnce() {
        UUID user = UUID.randomUUID();

        tracker.touch(user);
        tracker.touch(user);
        tracker.touch(user);

        verify(jdbcTemplate, times(1)).update(anyString(), any(Object.class));
    }

    @Test
    void eachUserCountsOnceAsConnected() {
        UUID user = UUID.randomUUID();

        tracker.touch(user);
        tracker.touch(user);
        tracker.touch(UUID.randomUUID());

        assertThat(tracker.connectedNow()).isEqualTo(2);
    }

    @Test
    void eachUserWritesTheirOwnActivity() {
        tracker.touch(UUID.randomUUID());
        tracker.touch(UUID.randomUUID());

        verify(jdbcTemplate, times(2)).update(anyString(), any(Object.class));
    }

    @Test
    void aDatabaseWithoutTheColumnNeitherFailsTheRequestNorIsAskedAgainInThePeriod() {
        UUID user = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), any(Object.class)))
                .thenThrow(new BadSqlGrammarException("touch", "update", new SQLException("no column")));

        assertThatCode(() -> tracker.touch(user)).doesNotThrowAnyException();
        tracker.touch(user);

        verify(jdbcTemplate, times(1)).update(anyString(), any(Object.class));
    }
}
