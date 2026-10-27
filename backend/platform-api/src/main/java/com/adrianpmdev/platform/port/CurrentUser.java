package com.adrianpmdev.platform.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Who is calling the API. Implemented by core (from the security context), so the game module needs no
 * security code and no dependency on core.
 *
 * @author adrianpmdev
 */
public interface CurrentUser {

    /**
     * Platform user id of the caller.
     *
     * @return the user id, or empty when nobody is signed in
     */
    Optional<UUID> id();

    /**
     * Whether the caller has the admin role.
     *
     * @return true for admins
     */
    boolean isAdmin();
}
