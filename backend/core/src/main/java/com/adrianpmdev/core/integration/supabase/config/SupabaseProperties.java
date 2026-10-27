package com.adrianpmdev.core.integration.supabase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Supabase connection settings (prefix {@code supabase}).
 *
 * @param url            project URL, e.g. https://xxxx.supabase.co
 * @param anonKey        anon / publishable key (used for public Auth calls)
 * @param serviceRoleKey service_role / secret key (used for admin Auth calls, never exposed)
 * @param jwt            access token verification settings
 *
 * @author adrianpmdev
 */
@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(
        String url,
        String anonKey,
        String serviceRoleKey,
        @DefaultValue Jwt jwt
) {

    /**
     * Access token verification settings.
     *
     * @param jwksUri   JWKS endpoint; defaults to {@code <url>/auth/v1/.well-known/jwks.json}
     * @param algorithm JWS algorithm used with the JWKS endpoint (ES256 for the default ECC key)
     * @param secret    legacy shared secret (HS256). When set it is used instead of the JWKS endpoint
     * @param issuer    expected issuer; defaults to {@code <url>/auth/v1}
     * @param audience  expected audience of the session tokens ({@code authenticated} in Supabase); blank skips the
     *                  check
     */
    public record Jwt(
            String jwksUri,
            @DefaultValue("ES256") String algorithm,
            String secret,
            String issuer,
            @DefaultValue("authenticated") String audience
    ) {
    }

    /**
     * Base URL of the Supabase Auth API without trailing slash.
     *
     * @return Auth API base URL
     */
    public String authUrl() {
        String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        return base + "/auth/v1";
    }

    /**
     * Resolves the JWKS URI, falling back to the project default.
     *
     * @return JWKS URI
     */
    public String resolvedJwksUri() {
        return hasText(jwt.jwksUri()) ? jwt.jwksUri() : authUrl() + "/.well-known/jwks.json";
    }

    /**
     * Resolves the expected token issuer, falling back to the project default.
     *
     * @return expected issuer
     */
    public String resolvedIssuer() {
        return hasText(jwt.issuer()) ? jwt.issuer() : authUrl();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}