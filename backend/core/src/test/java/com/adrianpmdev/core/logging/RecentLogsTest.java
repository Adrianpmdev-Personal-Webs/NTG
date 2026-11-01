package com.adrianpmdev.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which log lines are kept, how many and in what shape.
 *
 * @author adrianpmdev
 */
class RecentLogsTest {

    private final LoggerContext context = new LoggerContext();
    private final RecentLogs logs = new RecentLogs();

    @BeforeEach
    void setUp() {
        context.setMDCAdapter(new LogbackMDCAdapter());
        logs.setContext(context);
        logs.start();
    }

    @Test
    void onlyWarningsAndErrorsAreKeptNewestFirst() {
        logs.doAppend(event(Level.INFO, "fine"));
        logs.doAppend(event(Level.WARN, "careful"));
        logs.doAppend(event(Level.ERROR, "broken"));

        assertThat(logs.entries()).extracting(RecentLogs.Entry::message).containsExactly("broken", "careful");
        assertThat(logs.entries().getFirst().level()).isEqualTo("ERROR");
        assertThat(logs.entries().getFirst().source()).isEqualTo("SomeService");
    }

    @Test
    void theOldestLeaveWhenItIsFull() {
        for (int i = 0; i < RecentLogs.CAPACITY + 5; i++) {
            logs.doAppend(event(Level.WARN, "line " + i));
        }

        assertThat(logs.entries()).hasSize(RecentLogs.CAPACITY);
        assertThat(logs.entries().getFirst().message()).isEqualTo("line " + (RecentLogs.CAPACITY + 4));
        assertThat(logs.entries().getLast().message()).isEqualTo("line 5");
    }

    @Test
    void aLongMessageIsShortenedAndTheRequestIdIsKept() {
        LoggingEvent event = event(Level.WARN, "x".repeat(1000));
        event.setMDCPropertyMap(Map.of(RequestLoggingFilter.REQUEST_ID_KEY, "abc123"));

        logs.doAppend(event);

        assertThat(logs.entries().getFirst().message()).hasSizeLessThan(310);
        assertThat(logs.entries().getFirst().requestId()).isEqualTo("abc123");
    }

    @Test
    void theCauseOfAnErrorIsNamedWithoutItsTrace() {
        LoggingEvent event = new LoggingEvent("fqcn", context.getLogger("com.example.SomeService"), Level.ERROR,
                "failed", new IllegalStateException("no memory"), null);

        logs.doAppend(event);

        assertThat(logs.entries().getFirst().message()).isEqualTo("failed · IllegalStateException: no memory");
    }

    @Test
    void itListensToTheRealLogsWithTheirRequestId() {
        RecentLogs attached = new RecentLogs();
        attached.attach();

        try {
            MDC.put(RequestLoggingFilter.REQUEST_ID_KEY, "real-request");
            LoggerFactory.getLogger(RecentLogsTest.class).warn("a real warning");
        } finally {
            MDC.remove(RequestLoggingFilter.REQUEST_ID_KEY);
            attached.detach();
        }

        assertThat(attached.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.message()).isEqualTo("a real warning");
            assertThat(entry.source()).isEqualTo("RecentLogsTest");
            assertThat(entry.requestId()).isEqualTo("real-request");
        });
    }

    private LoggingEvent event(Level level, String message) {
        return new LoggingEvent("fqcn", context.getLogger("com.example.SomeService"), level, message, null, null);
    }
}
