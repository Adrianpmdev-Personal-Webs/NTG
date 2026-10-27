package com.adrianpmdev.core.features.auth.api.controller;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.adrianpmdev.core.features.auth.api.dto.request.LoginRequest;
import com.adrianpmdev.core.features.auth.api.dto.request.SetPasswordRequest;
import com.adrianpmdev.core.features.auth.api.dto.response.AuthResult;
import com.adrianpmdev.core.features.auth.api.dto.response.AuthTokensResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import com.adrianpmdev.core.security.cookie.RefreshTokenCookies;
import com.adrianpmdev.core.security.ratelimit.AuthRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import com.adrianpmdev.core.features.auth.application.service.AuthService;
import com.adrianpmdev.core.features.users.application.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for authentication and session management.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final UserService userService;
    private final RefreshTokenCookies refreshTokenCookies;
    private final AuthRateLimiter rateLimiter;

    /**
     * Builds the controller with its dependencies.
     *
     * @param authService         authentication service
     * @param userService         user service
     * @param refreshTokenCookies refresh cookie factory
     * @param rateLimiter         limits on sign-in attempts
     */
    public AuthController(AuthService authService, UserService userService, RefreshTokenCookies refreshTokenCookies,
                          AuthRateLimiter rateLimiter) {
        this.authService = authService;
        this.userService = userService;
        this.refreshTokenCookies = refreshTokenCookies;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Logs in using email and password. Sets the session cookie. Attempts are limited (429 when exceeded).
     *
     * @param request     login credentials
     * @param httpRequest current request (client address for the limits)
     * @return access token and user data
     */
    @PostMapping("/login")
    public ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request,
                                                    HttpServletRequest httpRequest) {
        rateLimiter.checkLogin(httpRequest, request.email());

        return withSession(authService.login(request.email(), request.password()));
    }

    /**
     * Restores the session from the refresh token cookie. The web client calls this when it starts
     * and shortly before the access token expires. Renewals are limited (429 when exceeded).
     *
     * @param refreshToken refresh token cookie
     * @param httpRequest  current request (client address for the limits)
     * @return new access token and user data
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokensResponse> refresh(
            @CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }

        rateLimiter.checkRefresh(httpRequest);

        return withSession(authService.refresh(refreshToken));
    }

    /**
     * Logs out: removes the cookie and, if an access token is sent, revokes the session in Supabase.
     *
     * @param authorization optional Bearer access token
     * @return empty response
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            authService.logout(authorization.substring(BEARER_PREFIX.length()).trim());
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.expire().toString())
                .build();
    }

    /**
     * Gets the authenticated user, including the tags that decide which games they can open.
     *
     * @param principal authenticated user
     * @return user data
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.getById(principal.id());
    }

    /**
     * Sets the password chosen from the email link and signs the user in.
     * Public endpoint: the token from the link is the proof of identity. Attempts are limited (429 when exceeded).
     *
     * @param request     access token from the link and the new password
     * @param httpRequest current request (client address for the limits)
     * @return access token and user data, plus the session cookie
     */
    @PutMapping("/set-password")
    public ResponseEntity<AuthTokensResponse> setPassword(@Valid @RequestBody SetPasswordRequest request,
                                                          HttpServletRequest httpRequest) {
        rateLimiter.checkSetPassword(httpRequest);

        return withSession(authService.setPassword(request.token(), request.password()));
    }

    private ResponseEntity<AuthTokensResponse> withSession(AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.create(result.refreshToken()).toString())
                .body(result.response());
    }
}