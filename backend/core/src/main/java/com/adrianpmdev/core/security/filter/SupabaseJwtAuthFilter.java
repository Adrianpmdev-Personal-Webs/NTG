package com.adrianpmdev.core.security.filter;

import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import com.adrianpmdev.core.security.authorization.Authorities;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import com.adrianpmdev.core.security.config.SecurityConfig;
import com.adrianpmdev.core.security.authorization.RoleUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Authenticates requests that carry a Supabase access token.
 * The token is verified locally; the role and tags are read from the local database on every request,
 * so changing a role or a tag (or deleting a user) takes effect immediately.
 * If the token is missing or invalid the request simply continues unauthenticated and the
 * authorization rules decide (public endpoints keep working, protected ones answer 401).
 * It is created in {@link SecurityConfig} and is deliberately not a {@code @Component}.
 *
 * @author adrianpmdev
 */
public class SupabaseJwtAuthFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SupabaseJwtAuthFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEFAULT_ROLE = "USUARIO";

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    /**
     * Builds the filter.
     *
     * @param jwtDecoder     access token decoder
     * @param userRepository local user repository
     */
    public SupabaseJwtAuthFilter(JwtDecoder jwtDecoder, UserRepository userRepository) {
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()).trim(), request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            UUID userId = UUID.fromString(jwt.getSubject());
            User user = userRepository.findWithTagsById(userId).orElse(null);

            if (user == null) {
                LOG.debug("Valid token for user {} without local profile", userId);
                return;
            }

            String role = RoleUtils.normalizeRole(user.getRole(), DEFAULT_ROLE);
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(Authorities.ROLE_PREFIX + role));

            for (Tag tag : user.getTags()) {
                authorities.add(new SimpleGrantedAuthority(Authorities.tag(tag.getKey())));
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    new AuthenticatedUser(userId, role), null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException exception) {
            LOG.debug("Rejected access token: {}", exception.getMessage());
        }
    }
}