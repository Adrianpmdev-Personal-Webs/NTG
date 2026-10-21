package com.adrianpmdev.core.security.authentication;

import com.adrianpmdev.core.security.authorization.Authorities;
import com.adrianpmdev.core.features.auth.application.service.AuthService;
import com.adrianpmdev.core.security.authorization.RoleUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Authentication provider for Swagger Basic Auth access.
 * Validates credentials against Supabase Auth and only allows users with ADMIN role.
 * Every Swagger request performs a Supabase sign-in, so disable Swagger in production.
 *
 * @author adrianpmdev
 */
@Component
public class SupabaseBasicAdminAuthenticationProvider implements AuthenticationProvider {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String EMPTY_CREDENTIALS_MESSAGE = "Empty credentials";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";
    private static final String NON_ADMIN_MESSAGE = "Authenticated user does not have admin role";

    private final AuthService authService;

    /**
     * Builds the provider with its dependencies.
     *
     * @param authService service used to validate credentials
     */
    public SupabaseBasicAdminAuthenticationProvider(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        Object credentials = authentication.getCredentials();
        String password = credentials == null ? "" : credentials.toString();

        if (email == null || email.isBlank() || password.isBlank()) {
            throw new BadCredentialsException(EMPTY_CREDENTIALS_MESSAGE);
        }

        try {
            String role = RoleUtils.normalizeRole(authService.authenticateAdminRole(email, password), "");

            if (!ADMIN_ROLE.equals(role)) {
                throw new BadCredentialsException(NON_ADMIN_MESSAGE);
            }

            return new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority(Authorities.ROLE_PREFIX + ADMIN_ROLE)));
        } catch (ResponseStatusException exception) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE, exception);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}