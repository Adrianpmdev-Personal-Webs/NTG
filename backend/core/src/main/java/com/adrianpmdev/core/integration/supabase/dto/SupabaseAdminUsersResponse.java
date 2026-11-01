package com.adrianpmdev.core.integration.supabase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * DTO that maps the list of accounts returned by the Supabase Auth admin API.
 *
 * @param users accounts
 *
 * @author adrianpmdev
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseAdminUsersResponse(
    @JsonProperty("users") List<Account> users
) {

    /**
     * An account.
     *
     * @param id           Supabase user UUID
     * @param lastSignInAt last sign in, or null when it never signed in
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(
        @JsonProperty("id") String id,
        @JsonProperty("last_sign_in_at") Instant lastSignInAt
    ) {}
}
