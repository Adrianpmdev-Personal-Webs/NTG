package com.adrianpmdev.core.web.health;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The keep-alive endpoint: always alive, and at most one database query a day.
 *
 * @author adrianpmdev
 */
class KeepAliveControllerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final KeepAliveController controller = new KeepAliveController(jdbcTemplate);

    @Test
    void onlyTheFirstCallOfTheDayQueriesTheDatabase() {
        for (int call = 0; call < 5; call++) {
            assertThat(controller.keepAlive().getStatusCode().is2xxSuccessful()).isTrue();
        }

        verify(jdbcTemplate, times(1)).queryForObject("select 1", Integer.class);
    }

    @Test
    void aFailedQueryIsTriedAgainOnTheNextCall() {
        when(jdbcTemplate.queryForObject("select 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("down"))
                .thenReturn(1);

        assertThat(controller.keepAlive().getStatusCode().is2xxSuccessful()).isTrue();
        controller.keepAlive();
        controller.keepAlive();

        verify(jdbcTemplate, times(2)).queryForObject("select 1", Integer.class);
    }
}
