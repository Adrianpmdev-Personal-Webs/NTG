package com.adrianpmdev.core.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Limits the attempts on the public session endpoints, so nobody can try passwords or tokens without end.
 * <p>
 * The backend signs in against Supabase from its own address, so Supabase sees every attempt as coming from the
 * server and its own per-address limits cannot protect each user. The limits live here instead, as token buckets
 * (Bucket4j): each key holds a few attempts that come back little by little.
 * <ul>
 *     <li><b>Sign in:</b> per email ({@value #LOGIN_PER_EMAIL} every 15 min), per client address
 *     ({@value #LOGIN_PER_ADDRESS} every 15 min) and for the whole site ({@value #LOGIN_GLOBAL} per minute).</li>
 *     <li><b>Session renewal:</b> per client address ({@value #REFRESH_PER_ADDRESS} per minute).</li>
 *     <li><b>Choosing a password from the email link:</b> per client address ({@value #PASSWORD_PER_ADDRESS} every
 *     15 min) and for the whole site ({@value #PASSWORD_GLOBAL} per minute).</li>
 * </ul>
 * The client address is the first one in {@code X-Forwarded-For} (the browser, as Vercel and Render forward it).
 * Whoever calls the server directly can fake that header, which is why the per-email and site-wide limits exist: they
 * do not depend on it. Buckets live in memory and are forgotten after an hour without use.
 *
 * @author adrianpmdev
 */
@Slf4j
@Component
public class AuthRateLimiter {

    static final int LOGIN_PER_EMAIL = 10;
    static final int LOGIN_PER_ADDRESS = 20;
    static final int LOGIN_GLOBAL = 100;
    static final int REFRESH_PER_ADDRESS = 30;
    static final int PASSWORD_PER_ADDRESS = 5;
    static final int PASSWORD_GLOBAL = 20;

    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final String FORWARDED_FOR = "X-Forwarded-For";
    private static final String GLOBAL_KEY = "*";

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(10_000)
            .build();

    /**
     * Counts a sign-in attempt.
     *
     * @param request current request
     * @param email   email being signed in
     * @throws TooManyRequestsException when a limit has been reached
     */
    public void checkLogin(HttpServletRequest request, String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        consume("login:email:" + normalizedEmail, LOGIN_PER_EMAIL, FIFTEEN_MINUTES);
        consume("login:address:" + clientAddress(request), LOGIN_PER_ADDRESS, FIFTEEN_MINUTES);
        consume("login:" + GLOBAL_KEY, LOGIN_GLOBAL, ONE_MINUTE);
    }

    /**
     * Counts a session renewal.
     *
     * @param request current request
     * @throws TooManyRequestsException when the limit has been reached
     */
    public void checkRefresh(HttpServletRequest request) {
        consume("refresh:address:" + clientAddress(request), REFRESH_PER_ADDRESS, ONE_MINUTE);
    }

    /**
     * Counts an attempt to choose a password from the email link.
     *
     * @param request current request
     * @throws TooManyRequestsException when a limit has been reached
     */
    public void checkSetPassword(HttpServletRequest request) {
        consume("password:address:" + clientAddress(request), PASSWORD_PER_ADDRESS, FIFTEEN_MINUTES);
        consume("password:" + GLOBAL_KEY, PASSWORD_GLOBAL, ONE_MINUTE);
    }

    private void consume(String key, int attempts, Duration period) {
        Bucket bucket = buckets.get(key, ignored -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(attempts).refillGreedy(attempts, period).build())
                .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long seconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            log.warn("Rate limit reached for {} (retry in {} s)", key.startsWith("login:email:") ? "login:email" : key,
                    seconds);
            throw new TooManyRequestsException(seconds);
        }
    }

    /** The browser address as forwarded by the proxies, or the direct peer when there is none. */
    static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR);

        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();

            if (!first.isEmpty()) {
                return first;
            }
        }

        return request.getRemoteAddr();
    }
}
