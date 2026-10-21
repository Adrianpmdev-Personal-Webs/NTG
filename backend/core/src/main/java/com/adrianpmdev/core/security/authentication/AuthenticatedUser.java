package com.adrianpmdev.core.security.authentication;


import java.util.UUID;

/**
 * Principal stored in the Spring Security context for authenticated API requests.
 * Inject it in controllers with {@code @AuthenticationPrincipal}.
 *
 * @param id   user id (Supabase user id)
 * @param role normalized role without the ROLE_ prefix, e.g. ADMIN
 *
 * @author adrianpmdev
 */
public record AuthenticatedUser(UUID id, String role) {}