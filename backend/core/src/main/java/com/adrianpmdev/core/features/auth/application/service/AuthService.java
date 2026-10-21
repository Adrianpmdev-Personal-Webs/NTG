package com.adrianpmdev.core.features.auth.application.service;

import com.adrianpmdev.core.features.auth.api.dto.response.AuthResult;
import com.adrianpmdev.core.features.auth.api.dto.response.AuthTokensResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.integration.supabase.dto.SupabaseTokenResponse;
import com.adrianpmdev.core.integration.supabase.dto.SupabaseUserResponse;
import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import com.adrianpmdev.core.features.users.api.mapper.UserMapper;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import com.adrianpmdev.core.integration.supabase.auth.client.SupabaseAuthClient;
import com.adrianpmdev.core.integration.supabase.auth.exception.SupabaseAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Authentication service integrated with Supabase Auth.
 * Manages login, session refresh, logout and first-time password setup.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
public class AuthService {

    private static final String DEFAULT_TOKEN_TYPE = "bearer";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";
    private static final String INVALID_SESSION_MESSAGE = "Invalid or expired session";
    private static final String INVALID_LINK_MESSAGE = "Invalid or expired link";

    private final UserRepository userRepository;
    private final SupabaseAuthClient supabase;

    /**
     * Builds the service with its dependencies.
     *
     * @param userRepository local user repository
     * @param supabase       Supabase Auth client
     */
    public AuthService(UserRepository userRepository, SupabaseAuthClient supabase) {
        this.userRepository = userRepository;
        this.supabase = supabase;
    }

    /**
     * Logs in using email and password.
     *
     * @param email    user email
     * @param password user password
     * @return authentication result with response body and refresh token
     */
    public AuthResult login(String email, String password) {
        SupabaseTokenResponse token;

        try {
            token = supabase.signInWithPassword(email.trim(), password);
        } catch (SupabaseAuthException exception) {
            log.warn("Login failed for {} (supabase status {})", maskEmail(email), exception.status());
            throw exception.toResponseStatus(INVALID_CREDENTIALS_MESSAGE);
        }

        UserResponse user = loadUserResponse(token.user());
        log.info("Login ok userId={}", user.id());

        return toAuthResult(token, user);
    }

    /**
     * Renews the session using the refresh token. Supabase rotates the refresh token on every call.
     *
     * @param refreshToken current refresh token
     * @return authentication result with new access token and new refresh token
     */
    public AuthResult refresh(String refreshToken) {
        SupabaseTokenResponse token;

        try {
            token = supabase.refresh(refreshToken);
        } catch (SupabaseAuthException exception) {
            log.debug("Refresh rejected (supabase status {})", exception.status());
            throw exception.toResponseStatus(INVALID_SESSION_MESSAGE);
        }

        // The refresh response already carries the Supabase user: no extra call needed.
        return toAuthResult(token, loadUserResponse(token.user()));
    }

    /**
     * Revokes the session in Supabase. Best effort: failures are only logged.
     *
     * @param accessToken access token of the session to close, may be null
     */
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            supabase.signOut(accessToken);
        } catch (SupabaseAuthException exception) {
            log.debug("Could not revoke session in Supabase (status {})", exception.status());
        }
    }

    /**
     * Sets the password using the access token from the email link and signs the user in.
     *
     * @param accessToken access token received in the email link
     * @param password    password chosen by the user
     * @return authentication result so the user does not have to log in again
     */
    public AuthResult setPassword(String accessToken, String password) {
        SupabaseUserResponse supabaseUser;

        try {
            supabaseUser = supabase.getUser(accessToken);
            supabase.updatePassword(accessToken, password);
        } catch (SupabaseAuthException exception) {
            if (exception.status() == 422) {
                // e.g. "New password should be different from the old password"
                throw new ResponseStatusException(HttpStatusCode.valueOf(422), exception.getMessage(), exception);
            }

            log.warn("Set-password failed (supabase status {})", exception.status());
            throw exception.toResponseStatus(INVALID_LINK_MESSAGE);
        }

        // Fails with 403 if the account is not registered in this application.
        loadUserResponse(supabaseUser);
        log.info("Password set userId={}", supabaseUser.id());

        return login(supabaseUser.email(), password);
    }

    /**
     * Checks Basic Auth credentials and returns the role. Used only to protect Swagger.
     *
     * @param email    user email
     * @param password user password
     * @return role stored for the user
     */
    public String authenticateAdminRole(String email, String password) {
        try {
            SupabaseTokenResponse token = supabase.signInWithPassword(email.trim(), password);

            return loadUserResponse(token.user()).role();
        } catch (SupabaseAuthException exception) {
            throw exception.toResponseStatus(INVALID_CREDENTIALS_MESSAGE);
        }
    }

    private UserResponse loadUserResponse(SupabaseUserResponse supabaseUser) {
        if (supabaseUser == null || supabaseUser.id() == null || supabaseUser.id().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
        }

        UUID userId = UUID.fromString(supabaseUser.id());

        return findResponse(userId);
    }

    private UserResponse findResponse(UUID userId) {
        User user = userRepository.findWithTagsById(userId)
                .orElseThrow(() -> {
                    log.warn("Authenticated Supabase user {} has no local profile", userId);
                    return new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not registered in this application");
                });

        return UserMapper.toResponse(user);
    }

    private AuthResult toAuthResult(SupabaseTokenResponse token, UserResponse user) {
        String tokenType = token.tokenType() != null ? token.tokenType() : DEFAULT_TOKEN_TYPE;
        AuthTokensResponse body = new AuthTokensResponse(token.accessToken(), token.expiresIn(), tokenType, user);

        return new AuthResult(body, token.refreshToken());
    }

    private static String maskEmail(String email) {
        int at = email == null ? -1 : email.indexOf('@');

        if (at <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(at);
    }
}