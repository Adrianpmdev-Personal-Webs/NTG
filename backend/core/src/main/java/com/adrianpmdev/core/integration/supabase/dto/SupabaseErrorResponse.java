package com.adrianpmdev.core.integration.supabase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO that maps the different error bodies returned by Supabase Auth.
 *
 * @param msg              message (most Auth errors)
 * @param message          message (some gateway errors)
 * @param errorDescription description (OAuth style errors)
 * @param error            error identifier
 * @param errorCode        stable machine readable code, e.g. {@code email_exists}
 *
 * @author adrianpmdev
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseErrorResponse(
    @JsonProperty("msg") String msg,
    @JsonProperty("message") String message,
    @JsonProperty("error_description") String errorDescription,
    @JsonProperty("error") String error,
    @JsonProperty("error_code") String errorCode
) {

    /**
     * Gets the first non-blank human readable message.
     *
     * @param fallback value used when the body carries no message
     * @return readable message
     */
    public String bestMessage(String fallback) {
        for (String candidate : new String[] { msg, message, errorDescription, error }) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }

        return fallback;
    }
}