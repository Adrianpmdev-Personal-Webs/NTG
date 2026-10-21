package com.adrianpmdev.core.integration.supabase.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Error returned by Supabase Auth (or network failure when {@code status} is 0).
 *
 * @author adrianpmdev
 */
public class SupabaseAuthException extends RuntimeException {

    private final int status;
    private final String errorCode;

    /**
     * Builds the exception.
     *
     * @param status    HTTP status returned by Supabase, or 0 for network failures
     * @param errorCode Supabase error code, may be null
     * @param message   readable message from Supabase
     * @param cause     original exception
     */
    public SupabaseAuthException(int status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public int status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    /**
     * Whether Supabase is unreachable or failing (not the caller's fault).
     *
     * @return true for network failures and 5xx responses
     */
    public boolean isUpstreamFailure() {
        return status == 0 || status >= 500;
    }

    /**
     * Translates the error into an HTTP error for our API without leaking Supabase details.
     *
     * @param unauthorizedMessage message used when the credentials/token are rejected
     * @return exception to throw
     */
    public ResponseStatusException toResponseStatus(String unauthorizedMessage) {
        if (status == 429) {
            return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, try again later", this);
        }

        if (isUpstreamFailure()) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Authentication provider unavailable", this);
        }

        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage, this);
    }
}