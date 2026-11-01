package com.adrianpmdev.core.logging;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

/**
 * Counts the requests since this backend started, for the admin dashboard. They are plain counters in memory, so they
 * cost nothing per request and start again at every restart. The keep-alive calls of the external cron are left out,
 * as in the access log, because they say nothing about who uses the site.
 *
 * @author adrianpmdev
 */
@Component
public class TrafficStats {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String POST = "POST";
    private static final int TOO_MANY_REQUESTS = 429;

    private final LongAdder requests = new LongAdder();
    private final LongAdder clientErrors = new LongAdder();
    private final LongAdder serverErrors = new LongAdder();
    private final LongAdder totalMillis = new LongAdder();
    private final LongAdder failedLogins = new LongAdder();
    private final LongAdder rateLimited = new LongAdder();

    /**
     * The counters at one moment.
     *
     * @param requests     requests answered
     * @param clientErrors of them, answered with 4xx
     * @param serverErrors of them, answered with 5xx
     * @param averageMillis average time to answer
     * @param failedLogins sign-in attempts refused
     * @param rateLimited  requests refused for going over a limit (429)
     */
    public record Snapshot(long requests, long clientErrors, long serverErrors, long averageMillis, long failedLogins,
                           long rateLimited) {
    }

    /**
     * Counts an answered request.
     *
     * @param method HTTP method
     * @param path   request path
     * @param status answer status
     * @param millis time to answer
     */
    public void record(String method, String path, int status, long millis) {
        requests.increment();
        totalMillis.add(millis);

        if (status >= 500) {
            serverErrors.increment();
        } else if (status >= 400) {
            clientErrors.increment();
        }

        if (status == TOO_MANY_REQUESTS) {
            rateLimited.increment();
        } else if (status >= 400 && status < 500 && POST.equals(method) && LOGIN_PATH.equals(path)) {
            failedLogins.increment();
        }
    }

    /**
     * The counters right now.
     *
     * @return snapshot
     */
    public Snapshot snapshot() {
        long answered = requests.sum();

        return new Snapshot(answered, clientErrors.sum(), serverErrors.sum(),
                answered == 0 ? 0 : totalMillis.sum() / answered, failedLogins.sum(), rateLimited.sum());
    }
}
