package com.adrianpmdev.core.security.authorization;

import java.util.Locale;

/**
 * Utility methods for normalizing roles used by Spring Security.
 *
 * @author adrianpmdev
 */
public final class RoleUtils {

    private static final String DEFAULT_ROLE = "USUARIO";
    private static final String ROLE_PREFIX = "ROLE_";

    private RoleUtils() {
    }

    /**
     * Normalizes a role for use in Spring Security authorities.
     * If the role is null or blank, the default role is used.
     * The result is converted to uppercase and the ROLE_ prefix is removed if
     * present.
     *
     * @param role        role to normalize
     * @param defaultRole fallback role used when the role is null or blank
     * @return normalized role without ROLE_ prefix
     */
    public static String normalizeRole(String role, String defaultRole) {
        String fallback = resolveDefaultRole(defaultRole);

        if (role == null || role.isBlank()) {
            return fallback;
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT).trim();

        if (normalizedRole.startsWith(ROLE_PREFIX)) {
            return normalizedRole.substring(ROLE_PREFIX.length());
        }

        return normalizedRole;
    }

    /**
     * Resolves the fallback role used during role normalization.
     *
     * @param defaultRole fallback role candidate
     * @return normalized fallback role
     */
    private static String resolveDefaultRole(String defaultRole) {
        if (defaultRole == null || defaultRole.isBlank()) {
            return DEFAULT_ROLE;
        }

        return defaultRole.toUpperCase(Locale.ROOT).trim();
    }
}