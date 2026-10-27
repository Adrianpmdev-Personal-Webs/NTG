package com.adrianpmdev.core.security.authentication;

import com.adrianpmdev.core.security.authorization.Authorities;
import com.adrianpmdev.pokemon.shared.port.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Tells game modules who is calling, from the security context filled by the access token filter.
 * Game modules depend on this port and never on Spring Security.
 *
 * @author adrianpmdev
 */
@Component
public class SecurityContextCurrentUser implements CurrentUser {

    private static final String ROLE_ADMIN = Authorities.ROLE_PREFIX + "ADMIN";

    @Override
    public Optional<UUID> id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user.id());
        }

        return Optional.empty();
    }

    @Override
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
    }
}
