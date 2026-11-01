package com.adrianpmdev.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Keeps the last {@value #CAPACITY} warnings and errors of the logs in memory, without their stack traces, for the admin
 * dashboard: it saves opening the logs of Render to see that something went wrong, and the request id leads to the full
 * trace there. The memory it can take is small and fixed, and they are lost at every restart.
 *
 * @author adrianpmdev
 */
@Component
public class RecentLogs extends AppenderBase<ILoggingEvent> {

    static final int CAPACITY = 50;

    private static final int MAX_MESSAGE_LENGTH = 300;

    private final Deque<Entry> entries = new ArrayDeque<>(CAPACITY);

    /**
     * A warning or an error.
     *
     * @param at        when it happened
     * @param level     WARN or ERROR
     * @param source    class that logged it
     * @param message   what it said, shortened
     * @param requestId request it belongs to, or null
     */
    public record Entry(Instant at, String level, String source, String message, String requestId) {
    }

    /** Starts listening to the logs. */
    @PostConstruct
    void attach() {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();

        if (factory instanceof LoggerContext context) {
            setContext(context);
            setName("recent-logs");
            start();
            context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(this);
        }
    }

    /** Stops listening to the logs. */
    @PreDestroy
    void detach() {
        if (getContext() instanceof LoggerContext context) {
            context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(this);
        }

        stop();
    }

    /**
     * The warnings and errors kept, newest first.
     *
     * @return entries
     */
    public List<Entry> entries() {
        synchronized (entries) {
            List<Entry> newestFirst = new ArrayList<>(entries);
            Collections.reverse(newestFirst);

            return newestFirst;
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!event.getLevel().isGreaterOrEqual(Level.WARN)) {
            return;
        }

        Entry entry = new Entry(Instant.ofEpochMilli(event.getTimeStamp()), event.getLevel().toString(),
                shortName(event.getLoggerName()), message(event),
                event.getMDCPropertyMap().get(RequestLoggingFilter.REQUEST_ID_KEY));

        synchronized (entries) {
            if (entries.size() == CAPACITY) {
                entries.removeFirst();
            }

            entries.addLast(entry);
        }
    }

    private static String message(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        IThrowableProxy cause = event.getThrowableProxy();

        if (cause != null) {
            message += " · " + shortName(cause.getClassName()) + ": " + cause.getMessage();
        }

        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH) + "…";
    }

    private static String shortName(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
