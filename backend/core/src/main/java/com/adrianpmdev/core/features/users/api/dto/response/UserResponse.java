package com.adrianpmdev.core.features.users.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO with user data exposed by the API.
 *
 * @param id        user identifier
 * @param email     user email
 * @param username  local username
 * @param avatarUrl avatar URL
 * @param role      user role
 * @param tags      tag keys granted to the user (e.g. the games they can access)
 * @param createdAt creation instant
 *
 * @author adrianpmdev
 */
public record UserResponse(
    UUID id,
    String email,
    String username,
    String avatarUrl,
    String role,
    List<String> tags,
    Instant createdAt
) {}