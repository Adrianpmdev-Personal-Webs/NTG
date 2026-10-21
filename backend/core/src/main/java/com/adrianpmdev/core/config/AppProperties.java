package com.adrianpmdev.core.config;

import com.adrianpmdev.core.Application;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Application settings (prefix {@code app}).
 *
 * @param frontend web client settings
 * @param cors     CORS settings
 * @param auth     session cookie settings
 *
 * @author adrianpmdev
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue Frontend frontend,
        @DefaultValue Cors cors,
        @DefaultValue Auth auth
) {

    /**
     * Web client settings.
     *
     * @param url             public URL of the web client
     * @param setPasswordPath client route that receives the Supabase email link
     */
    public record Frontend(
            @DefaultValue("http://localhost:4200") String url,
            @DefaultValue("/set-password") String setPasswordPath
    ) {
    }

    /**
     * CORS settings.
     *
     * @param allowedOrigins origins allowed to call the API with credentials
     */
    public record Cors(
            @DefaultValue({"http://localhost:4200"}) List<String> allowedOrigins
    ) {
    }

    /**
     * Refresh token cookie settings.
     *
     * @param cookieSecure                 send the cookie only over HTTPS (true in production)
     * @param cookieSameSite               Lax, Strict or None (None requires cookieSecure=true)
     * @param refreshCookieMaxAgeSeconds   cookie lifetime in seconds
     */
    public record Auth(
            @DefaultValue("false") boolean cookieSecure,
            @DefaultValue("Lax") String cookieSameSite,
            @DefaultValue("2592000") long refreshCookieMaxAgeSeconds
    ) {
    }

    /**
     * URL the Supabase email link redirects to.
     *
     * @return absolute set-password URL
     */
    public String setPasswordUrl() {
        String base = frontend.url().endsWith("/")
                ? frontend.url().substring(0, frontend.url().length() - 1)
                : frontend.url();

        return base + frontend.setPasswordPath();
    }
}