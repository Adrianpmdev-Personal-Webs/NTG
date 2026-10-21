package com.adrianpmdev.core.integration.supabase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO that maps the response from the Supabase /auth/v1/token endpoint.
 *
 * @param accessToken  JWT access token
 * @param refreshToken refresh token
 * @param expiresIn    token validity duration in seconds
 * @param tokenType    token type
 * @param user         user data returned by Supabase
 *
 * @author adrianpmdev
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") Long expiresIn,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("user") SupabaseUserResponse user
) {}