package com.adrianpmdev.core.security.ratelimit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * A 429 answer that tells the client, in the {@code Retry-After} header, how many seconds to wait.
 *
 * @author adrianpmdev
 */
public class TooManyRequestsException extends ResponseStatusException {

    private final long retryAfterSeconds;

    /**
     * Builds the exception.
     *
     * @param retryAfterSeconds seconds until the next attempt is allowed
     */
    public TooManyRequestsException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Vuelve a intentarlo en unos minutos");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));

        return headers;
    }
}
