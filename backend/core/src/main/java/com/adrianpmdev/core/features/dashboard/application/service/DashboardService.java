package com.adrianpmdev.core.features.dashboard.application.service;

import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.core.features.catalog.application.service.GameCatalogService;
import com.adrianpmdev.core.features.dashboard.api.dto.response.DashboardResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.features.users.application.service.UserService;
import com.adrianpmdev.core.integration.supabase.auth.client.SupabaseAuthClient;
import com.adrianpmdev.core.integration.supabase.dto.SupabaseAdminUsersResponse;
import com.adrianpmdev.core.logging.RecentLogs;
import com.adrianpmdev.core.logging.TrafficStats;
import com.adrianpmdev.core.security.authentication.UserActivityTracker;
import com.adrianpmdev.core.security.authorization.RoleUtils;
import com.adrianpmdev.platform.status.ModuleStatus;
import com.adrianpmdev.platform.status.ModuleStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the admin dashboard from real data, only when an admin opens or refreshes it, so an unattended backend spends
 * nothing on it. Each source is read on its own and a failing one is left empty, never hiding the others:
 * <ul>
 *     <li><b>Server:</b> the JVM and {@code /proc}, no call at all. The memory of the whole process is what counts
 *     against the plan, not the Java heap.</li>
 *     <li><b>Storage:</b> two small queries (size of the database, files per bucket).</li>
 *     <li><b>Modules:</b> whatever each {@link ModuleStatusProvider} answers from memory.</li>
 *     <li><b>Users:</b> the local accounts, one query for their last activity (see UserActivityTracker) and one call
 *     to the admin API of Supabase Auth for the last sign ins, which also tells who has never entered.</li>
 *     <li><b>Traffic and logs:</b> counters and the latest warnings and errors in memory, since the last start.</li>
 * </ul>
 * The plan limits are settings because they belong to the plan, not to this code.
 *
 * @author adrianpmdev
 */
@Service
public class DashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardService.class);

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String DEFAULT_ROLE = "USUARIO";
    private static final Path PROCESS_STATUS = Path.of("/proc/self/status");
    private static final int VERSION_LENGTH = 7;
    private static final long MEGABYTE = 1024L * 1024;

    private static final String DATABASE_SIZE = "select pg_database_size(current_database())";
    private static final String BUCKET_SIZES = """
            select bucket_id, count(*) as objects, coalesce(sum((metadata->>'size')::bigint), 0) as bytes
            from storage.objects
            group by bucket_id
            order by bytes desc
            """;
    private static final String LAST_SEEN = "select id, last_seen_at from core.\"user\" where last_seen_at is not null";

    private final UserService userService;
    private final GameCatalogService gameCatalogService;
    private final SupabaseAuthClient authClient;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<ModuleStatusProvider> statusProviders;
    private final TrafficStats trafficStats;
    private final RecentLogs recentLogs;
    private final UserActivityTracker activityTracker;
    private final String commit;
    private final long memoryLimitBytes;
    private final long databaseLimitBytes;
    private final long storageLimitBytes;

    /**
     * Builds the service with its dependencies.
     *
     * @param userService        the local accounts
     * @param gameCatalogService game names
     * @param authClient         last sign ins
     * @param jdbcTemplate       plain JDBC access for the sizes
     * @param statusProviders    modules that declare a state
     * @param trafficStats       counters of the requests
     * @param recentLogs         latest warnings and errors
     * @param activityTracker    users connected now
     * @param commit             commit deployed, which Render gives to the process
     * @param memoryLimitMb      memory of the Render plan
     * @param databaseLimitMb    database space of the Supabase plan
     * @param storageLimitMb     file space of the Supabase plan
     */
    public DashboardService(UserService userService, GameCatalogService gameCatalogService,
                            SupabaseAuthClient authClient, JdbcTemplate jdbcTemplate,
                            ObjectProvider<ModuleStatusProvider> statusProviders, TrafficStats trafficStats,
                            RecentLogs recentLogs, UserActivityTracker activityTracker,
                            @Value("${RENDER_GIT_COMMIT:}") String commit,
                            @Value("${app.dashboard.memory-limit-mb:512}") long memoryLimitMb,
                            @Value("${app.dashboard.database-limit-mb:500}") long databaseLimitMb,
                            @Value("${app.dashboard.storage-limit-mb:1024}") long storageLimitMb) {
        this.userService = userService;
        this.gameCatalogService = gameCatalogService;
        this.authClient = authClient;
        this.jdbcTemplate = jdbcTemplate;
        this.statusProviders = statusProviders;
        this.trafficStats = trafficStats;
        this.recentLogs = recentLogs;
        this.activityTracker = activityTracker;
        this.commit = commit;
        this.memoryLimitBytes = memoryLimitMb * MEGABYTE;
        this.databaseLimitBytes = databaseLimitMb * MEGABYTE;
        this.storageLimitBytes = storageLimitMb * MEGABYTE;
    }

    /**
     * The dashboard as it is right now.
     *
     * @return dashboard data
     */
    public DashboardResponse get() {
        return new DashboardResponse(server(), storage(), modules(), users(),
                new DashboardResponse.Traffic(trafficStats.snapshot(), activityTracker.connectedNow()),
                recentLogs.entries());
    }

    /* ─── Server ─── */

    private DashboardResponse.Server server() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        String version = commit.isBlank() ? null : commit.substring(0, Math.min(VERSION_LENGTH, commit.length()));

        return new DashboardResponse.Server(version, ManagementFactory.getRuntimeMXBean().getUptime() / 1000,
                cpuPercent(), processBytes(), heap.getUsed(), heap.getMax(), memoryLimitBytes);
    }

    private static Double cpuPercent() {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean system) {
            double load = system.getProcessCpuLoad();

            return load < 0 ? null : load * 100;
        }

        return null;
    }

    private static Long processBytes() {
        if (!Files.isReadable(PROCESS_STATUS)) {
            return null;
        }

        try {
            return residentBytes(Files.readAllLines(PROCESS_STATUS));
        } catch (IOException | RuntimeException exception) {
            LOG.warn("The memory of the process could not be read: {}", exception.getMessage());

            return null;
        }
    }

    /** Reads the line {@code VmRSS:  123456 kB} of the status of a process. */
    static Long residentBytes(List<String> status) {
        return status.stream()
                .filter(line -> line.startsWith("VmRSS:"))
                .findFirst()
                .map(line -> Long.parseLong(line.replaceAll("\\D", "")) * 1024)
                .orElse(null);
    }

    /* ─── Storage ─── */

    private DashboardResponse.Storage storage() {
        Long databaseBytes = databaseBytes();
        List<DashboardResponse.Bucket> buckets = buckets();
        Long storageBytes = buckets == null ? null
                : buckets.stream().mapToLong(DashboardResponse.Bucket::bytes).sum();

        return new DashboardResponse.Storage(databaseBytes, databaseLimitBytes, storageBytes, storageLimitBytes,
                buckets == null ? List.of() : buckets);
    }

    private Long databaseBytes() {
        try {
            return jdbcTemplate.queryForObject(DATABASE_SIZE, Long.class);
        } catch (RuntimeException exception) {
            LOG.warn("The size of the database could not be read: {}", exception.getMessage());

            return null;
        }
    }

    private List<DashboardResponse.Bucket> buckets() {
        try {
            return jdbcTemplate.query(BUCKET_SIZES, (row, index) -> new DashboardResponse.Bucket(
                    row.getString("bucket_id"), row.getLong("objects"), row.getLong("bytes")));
        } catch (RuntimeException exception) {
            LOG.warn("The size of the buckets could not be read: {}", exception.getMessage());

            return null;
        }
    }

    /* ─── Modules ─── */

    private List<ModuleStatus> modules() {
        List<ModuleStatus> modules = new ArrayList<>();

        for (ModuleStatusProvider provider : statusProviders.orderedStream().toList()) {
            try {
                modules.add(provider.moduleStatus());
            } catch (RuntimeException exception) {
                LOG.warn("A module could not tell its state: {}", exception.getMessage());
            }
        }

        return modules;
    }

    /* ─── Users ─── */

    private DashboardResponse.Users users() {
        List<UserResponse> users = userService.getAll();
        Map<String, Instant> signIns = signIns();
        boolean known = signIns != null;
        Map<String, Instant> lastSignIns = known ? signIns : Map.of();
        Map<String, Instant> lastSeen = lastSeen();

        List<DashboardResponse.Account> accounts = users.stream()
                .sorted(Comparator.comparing(UserResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(user -> new DashboardResponse.Account(user.username(), user.email(), user.role(),
                        user.createdAt(), latest(lastSeen.get(user.id().toString()), lastSignIns.get(user.id().toString()))))
                .toList();

        int admins = (int) users.stream().filter(DashboardService::isAdmin).count();
        Integer neverSignedIn = known
                ? (int) accounts.stream().filter(account -> account.lastActiveAt() == null).count()
                : null;

        return new DashboardResponse.Users(users.size(), admins, users.size() - admins, known, neverSignedIn,
                perGame(users), accounts);
    }

    /** Last sign in by account id, or null when Supabase cannot tell. Accounts that never signed in are left out. */
    private Map<String, Instant> signIns() {
        try {
            SupabaseAdminUsersResponse response = authClient.adminListUsers();
            Map<String, Instant> signIns = new HashMap<>();

            if (response == null || response.users() == null) {
                return null;
            }

            for (SupabaseAdminUsersResponse.Account account : response.users()) {
                if (account.lastSignInAt() != null) {
                    signIns.put(account.id(), account.lastSignInAt());
                }
            }

            return signIns;
        } catch (RuntimeException exception) {
            LOG.warn("The last sign ins could not be read: {}", exception.getMessage());

            return null;
        }
    }

    /** Last request by account id; empty when the column cannot be read, which only leaves the sign ins. */
    private Map<String, Instant> lastSeen() {
        Map<String, Instant> lastSeen = new HashMap<>();

        try {
            jdbcTemplate.query(LAST_SEEN, (RowCallbackHandler) row ->
                    lastSeen.put(row.getString("id"), row.getTimestamp("last_seen_at").toInstant()));
        } catch (RuntimeException exception) {
            LOG.warn("The last activity of the users could not be read: {}", exception.getMessage());
        }

        return lastSeen;
    }

    private static Instant latest(Instant first, Instant second) {
        if (first == null || second == null) {
            return first == null ? second : first;
        }

        return first.isAfter(second) ? first : second;
    }

    private List<DashboardResponse.GameUsers> perGame(List<UserResponse> users) {
        List<GameResponse> games = gameCatalogService.getVisible(true, Set.of());

        return games.stream()
                .map(game -> new DashboardResponse.GameUsers(game.name(), game.iconUrl(),
                        (int) users.stream().filter(user -> user.tags().contains(game.id())).count()))
                .toList();
    }

    private static boolean isAdmin(UserResponse user) {
        return ADMIN_ROLE.equals(RoleUtils.normalizeRole(user.role(), DEFAULT_ROLE));
    }
}
