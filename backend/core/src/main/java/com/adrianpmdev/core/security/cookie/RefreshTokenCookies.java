package com.adrianpmdev.core.security.cookie;

import com.adrianpmdev.core.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the HttpOnly cookie that keeps the session between visits.
 * Only the refresh token lives in the cookie; the access token is kept in memory by the web client.
 *
 * @author adrianpmdev
 */
@Component
public class RefreshTokenCookies {

    public static final String NAME = "refresh_token";

    private static final String PATH = "/api/v1/auth";

    private final AppProperties.Auth settings;

    /**
     * Builds the factory.
     *
     * @param appProperties application settings
     */
    public RefreshTokenCookies(AppProperties appProperties) {
        this.settings = appProperties.auth();
    }

    /**
     * Builds the cookie that stores a refresh token.
     *
     * @param refreshToken refresh token value
     * @return cookie
     */
    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(settings.refreshCookieMaxAgeSeconds()).build();
    }

    /**
     * Builds the cookie that removes the stored refresh token.
     *
     * @return expired cookie
     */
    public ResponseCookie expire() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(settings.cookieSecure())
                .sameSite(settings.cookieSameSite())
                .path(PATH);
    }
}