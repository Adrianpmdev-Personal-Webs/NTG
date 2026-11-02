package com.adrianpmdev.platform.module;

import java.util.UUID;

/**
 * Implemented by a game that keeps something about a platform user. Core collects every cleaner and calls them before
 * deleting an account, so it never names a game. A game deletes what only belongs to that person and keeps, without the
 * link to the account, what other players need for their own history.
 *
 * @author adrianpmdev
 */
public interface UserDataCleaner {

    /**
     * Removes what the game keeps about a user. It must be safe to call again, because a failed deletion can be retried.
     *
     * @param userId platform user id of the account being deleted
     */
    void removeUser(UUID userId);
}
