package com.adrianpmdev.core.integration.supabase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO that maps user data returned by Supabase Auth.
 *
 * @param id    Supabase user UUID
 * @param email user email
 *
 * @author adrianpmdev
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseUserResponse(
    @JsonProperty("id") String id,
    @JsonProperty("email") String email
) {}