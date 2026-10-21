package com.adrianpmdev.core.features.auth.api.dto.response;

/**
 * Internal authentication result containing the response body and the refresh token cookie value.
 *
 * @param response authentication response body
 * @param refreshToken refresh token to store in an HttpOnly cookie
 *
 * @author adrianpmdev
 */
public record AuthResult(
        AuthTokensResponse response,
        String refreshToken
) {}