package com.adrianpmdev.platform.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * The names of the platform accounts, for a game that shows who owns something. Implemented by core, so the game module
 * needs no dependency on core and never sees an email or a role.
 *
 * @author adrianpmdev
 */
public interface UserDirectory {

    /**
     * Names of some accounts.
     *
     * @param ids account ids
     * @return username by id; an id without account is left out
     */
    Map<UUID, String> usernames(Collection<UUID> ids);

    /**
     * The accounts with a game tag.
     *
     * @param tag key of the tag
     * @return username by id, sorted by username
     */
    Map<UUID, String> usersWithTag(String tag);
}
