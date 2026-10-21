package com.adrianpmdev.core.features.auth.api.dto.response;

import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;

/**
 * Response DTO for authentication responses.
 *
 * @param accessToken JWT access token
 * @param expiresIn access token validity duration in seconds
 * @param tokenType returned token type
 * @param user authenticated local user
 *
 * @author adrianpmdev
 */
public record AuthTokensResponse(
        String accessToken,
        Long expiresIn,
        String tokenType,
        UserResponse user
) {}