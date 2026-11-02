package com.adrianpmdev.lol.features.riot;

/**
 * Riot could not answer. The message is safe to show: it never carries the key or a URL.
 *
 * @author adrianpmdev
 */
public class RiotApiException extends RuntimeException {

    private final int status;

    /**
     * Builds the exception.
     *
     * @param status  HTTP status of Riot, or 0 when there was no answer
     * @param message reason, in Spanish
     */
    public RiotApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * HTTP status of Riot.
     *
     * @return the status, or 0 when there was no answer
     */
    public int status() {
        return status;
    }
}
