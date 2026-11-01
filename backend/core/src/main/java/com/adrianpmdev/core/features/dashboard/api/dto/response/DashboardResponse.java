package com.adrianpmdev.core.features.dashboard.api.dto.response;

import com.adrianpmdev.core.logging.RecentLogs;
import com.adrianpmdev.core.logging.TrafficStats;
import com.adrianpmdev.platform.status.ModuleStatus;

import java.time.Instant;
import java.util.List;

/**
 * Everything the admin dashboard shows. A value that could not be read is null, so one failing source never hides the
 * others.
 *
 * @param server  the backend process
 * @param storage space used in Supabase
 * @param modules state of the modules that declare one
 * @param users   the accounts
 * @param traffic requests since this backend started
 * @param logs    latest warnings and errors, newest first
 *
 * @author adrianpmdev
 */
public record DashboardResponse(
    Server server,
    Storage storage,
    List<ModuleStatus> modules,
    Users users,
    Traffic traffic,
    List<RecentLogs.Entry> logs
) {

    /**
     * Requests since this backend started.
     *
     * @param counters what the requests answered
     * @param connected users that made a request in the last few minutes
     */
    public record Traffic(TrafficStats.Snapshot counters, long connected) {}

    /**
     * The backend process.
     *
     * @param version          commit deployed, or null outside Render
     * @param uptimeSeconds    time since this process started
     * @param cpuPercent       CPU used by the process, or null when unknown
     * @param processBytes     memory of the whole process, or null outside Linux
     * @param heapUsedBytes    Java heap in use
     * @param heapMaxBytes     largest Java heap allowed
     * @param memoryLimitBytes memory of the plan
     */
    public record Server(
        String version,
        long uptimeSeconds,
        Double cpuPercent,
        Long processBytes,
        long heapUsedBytes,
        long heapMaxBytes,
        long memoryLimitBytes
    ) {}

    /**
     * Space used in Supabase.
     *
     * @param databaseBytes      size of the database, or null when unknown
     * @param databaseLimitBytes database space of the plan
     * @param storageBytes       size of all the files, or null when unknown
     * @param storageLimitBytes  file space of the plan
     * @param buckets            files per bucket, biggest first
     */
    public record Storage(
        Long databaseBytes,
        long databaseLimitBytes,
        Long storageBytes,
        long storageLimitBytes,
        List<Bucket> buckets
    ) {}

    /**
     * Files of a bucket.
     *
     * @param name    bucket name
     * @param objects number of files
     * @param bytes   their size
     */
    public record Bucket(String name, long objects, long bytes) {}

    /**
     * The accounts.
     *
     * @param total          all of them
     * @param admins         admins among them
     * @param players        the others
     * @param signInsKnown   false when Supabase did not say who has signed in, so "never" cannot be told
     * @param neverSignedIn  accounts that have never signed in, or null when unknown
     * @param perGame        how many hold the tag of each game
     * @param accounts       every account, newest first
     */
    public record Users(
        int total,
        int admins,
        int players,
        boolean signInsKnown,
        Integer neverSignedIn,
        List<GameUsers> perGame,
        List<Account> accounts
    ) {}

    /**
     * Holders of the tag of a game.
     *
     * @param name    game name
     * @param iconUrl icon of the game, or null when it has none
     * @param users   how many
     */
    public record GameUsers(String name, String iconUrl, int users) {}

    /**
     * An account.
     *
     * @param username     username
     * @param email        email
     * @param role         role
     * @param createdAt    when it was created
     * @param lastActiveAt latest of its last request and its last sign in, or null when it has neither or it is
     *                     unknown
     */
    public record Account(String username, String email, String role, Instant createdAt, Instant lastActiveAt) {}
}
