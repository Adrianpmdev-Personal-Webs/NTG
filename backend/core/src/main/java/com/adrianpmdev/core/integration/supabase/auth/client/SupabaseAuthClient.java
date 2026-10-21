package com.adrianpmdev.core.integration.supabase.auth.client;

import com.adrianpmdev.core.integration.supabase.auth.exception.SupabaseAuthException;
import com.adrianpmdev.core.integration.supabase.config.SupabaseProperties;
import com.adrianpmdev.core.integration.supabase.dto.SupabaseErrorResponse;
import com.adrianpmdev.core.integration.supabase.dto.SupabaseTokenResponse;
import com.adrianpmdev.core.integration.supabase.dto.SupabaseUserResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * Thin HTTP client for the Supabase Auth (GoTrue) API.
 * It is the only class that knows endpoints, API keys and error formats.
 *
 * @author adrianpmdev
 */
@Component
public class SupabaseAuthClient {

    private static final String API_KEY_HEADER = "apikey";
    private static final String NEW_STYLE_KEY_PREFIX = "sb_";

    private final RestTemplate restTemplate;
    private final SupabaseProperties properties;

    /**
     * Builds the client.
     *
     * @param restTemplate shared HTTP client with timeouts
     * @param properties   Supabase settings
     */
    public SupabaseAuthClient(RestTemplate restTemplate, SupabaseProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Email + password sign in.
     *
     * @param email    user email
     * @param password user password
     * @return tokens and Supabase user
     */
    public SupabaseTokenResponse signInWithPassword(String email, String password) {
        return post("/token?grant_type=password", anonHeaders(),
                Map.of("email", email, "password", password), SupabaseTokenResponse.class);
    }

    /**
     * Exchanges a refresh token for a new session. Supabase rotates the refresh token.
     *
     * @param refreshToken current refresh token
     * @return new tokens and Supabase user
     */
    public SupabaseTokenResponse refresh(String refreshToken) {
        return post("/token?grant_type=refresh_token", anonHeaders(),
                Map.of("refresh_token", refreshToken), SupabaseTokenResponse.class);
    }

    /**
     * Revokes the session of the given access token (this device only).
     *
     * @param accessToken access token of the session to close
     */
    public void signOut(String accessToken) {
        HttpHeaders headers = anonHeaders();
        headers.setBearerAuth(accessToken);

        post("/logout?scope=local", headers, Map.of(), Void.class);
    }

    /**
     * Gets the user that owns an access token.
     *
     * @param accessToken access token
     * @return Supabase user
     */
    public SupabaseUserResponse getUser(String accessToken) {
        HttpHeaders headers = anonHeaders();
        headers.setBearerAuth(accessToken);

        return execute(properties.authUrl() + "/user", HttpMethod.GET, new HttpEntity<>(headers), SupabaseUserResponse.class);
    }

    /**
     * Sets the password of the user that owns the access token.
     *
     * @param accessToken  access token obtained from the email link
     * @param newPassword  new password
     */
    public void updatePassword(String accessToken, String newPassword) {
        HttpHeaders headers = anonHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        execute(properties.authUrl() + "/user", HttpMethod.PUT,
                new HttpEntity<>(Map.of("password", newPassword), headers), Void.class);
    }

    /**
     * Sends the password email (Supabase "Reset password" template) to an existing user.
     * Works for first-time passwords and for resets.
     *
     * @param email      target email
     * @param redirectTo URL the email link points to (must be in Supabase Redirect URLs)
     */
    public void sendPasswordEmail(String email, String redirectTo) {
        String url = UriComponentsBuilder.fromUriString(properties.authUrl() + "/recover")
                .queryParam("redirect_to", redirectTo)
                .build().encode().toUriString();
        HttpHeaders headers = anonHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        execute(url, HttpMethod.POST, new HttpEntity<>(Map.of("email", email), headers), Void.class);
    }

    /**
     * Creates a confirmed user without password using the admin API.
     *
     * @param email user email
     * @return id assigned by Supabase
     */
    public UUID adminCreateUser(String email) {
        SupabaseUserResponse created = post("/admin/users", adminHeaders(),
                Map.of("email", email, "email_confirm", true), SupabaseUserResponse.class);

        if (created == null || created.id() == null || created.id().isBlank()) {
            throw new SupabaseAuthException(502, null, "Supabase did not return the user id", null);
        }

        return UUID.fromString(created.id());
    }

    /**
     * Deletes a user using the admin API. A missing user is not an error.
     *
     * @param userId user id
     */
    public void adminDeleteUser(UUID userId) {
        try {
            execute(properties.authUrl() + "/admin/users/" + userId, HttpMethod.DELETE,
                    new HttpEntity<>(adminHeaders()), Void.class);
        } catch (SupabaseAuthException exception) {
            if (exception.status() != 404) {
                throw exception;
            }
        }
    }

    private <T> T post(String path, HttpHeaders headers, Object body, Class<T> responseType) {
        headers.setContentType(MediaType.APPLICATION_JSON);

        return execute(properties.authUrl() + path, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    private <T> T execute(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) {
        try {
            return restTemplate.exchange(url, method, entity, responseType).getBody();
        } catch (HttpStatusCodeException exception) {
            SupabaseErrorResponse error = readError(exception);
            String message = error != null ? error.bestMessage("Supabase error") : "Supabase error";
            String code = error != null ? error.errorCode() : null;

            throw new SupabaseAuthException(exception.getStatusCode().value(), code, message, exception);
        } catch (RestClientException exception) {
            throw new SupabaseAuthException(0, null, "Supabase unreachable", exception);
        }
    }

    private SupabaseErrorResponse readError(HttpStatusCodeException exception) {
        try {
            return exception.getResponseBodyAs(SupabaseErrorResponse.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private HttpHeaders anonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, properties.anonKey());

        return headers;
    }

    private HttpHeaders adminHeaders() {
        String key = properties.serviceRoleKey();
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, key);

        // Legacy service_role keys are JWTs and go in Authorization too; new sb_secret_ keys are not JWTs.
        if (!key.startsWith(NEW_STYLE_KEY_PREFIX)) {
            headers.setBearerAuth(key);
        }

        return headers;
    }
}