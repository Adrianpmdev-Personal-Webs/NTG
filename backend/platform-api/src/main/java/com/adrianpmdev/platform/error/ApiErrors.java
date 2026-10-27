package com.adrianpmdev.platform.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

/**
 * Exceptions the global handler of core turns into problem-detail responses: 404 for missing resources,
 * and the given status and message for deliberate business errors.
 *
 * @author adrianpmdev
 */
public final class ApiErrors {

    private ApiErrors() {
    }

    /**
     * A resource that does not exist (404).
     *
     * @param resource name of the resource, for the logs
     * @return exception to throw
     */
    public static NoSuchElementException notFound(String resource) {
        return new NoSuchElementException(resource + " not found");
    }

    /**
     * A request that breaks a rule of the current state (409).
     *
     * @param message message for the client
     * @return exception to throw
     */
    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    /**
     * A request with invalid data that bean validation cannot catch (400).
     *
     * @param message message for the client
     * @return exception to throw
     */
    public static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * A caller who may not do this (403).
     *
     * @return exception to throw
     */
    public static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
}
