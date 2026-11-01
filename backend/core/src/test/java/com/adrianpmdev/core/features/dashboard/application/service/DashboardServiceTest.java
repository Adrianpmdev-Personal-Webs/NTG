package com.adrianpmdev.core.features.dashboard.application.service;

import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.core.features.catalog.application.service.GameCatalogService;
import com.adrianpmdev.core.features.dashboard.api.dto.response.DashboardResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.features.users.application.service.UserService;
import com.adrianpmdev.core.integration.supabase.auth.client.SupabaseAuthClient;
import com.adrianpmdev.core.integration.supabase.dto.SupabaseAdminUsersResponse;
import com.adrianpmdev.platform.status.ModuleStatus;
import com.adrianpmdev.platform.status.ModuleStatusProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What the dashboard shows from each source, and that a failing source never hides the others.
 *
 * @author adrianpmdev
 */
class DashboardServiceTest {

    private static final UUID OLD_ID = UUID.randomUUID();
    private static final UUID NEW_ID = UUID.randomUUID();
    private static final Instant SIGN_IN = Instant.parse("2026-10-30T10:00:00Z");

    private final UserService userService = mock(UserService.class);
    private final GameCatalogService catalog = mock(GameCatalogService.class);
    private final SupabaseAuthClient authClient = mock(SupabaseAuthClient.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ModuleStatusProvider> providers = mock(ObjectProvider.class);

    private final DashboardService service = new DashboardService(userService, catalog, authClient, jdbcTemplate,
            providers, "abcdef1234567", 512, 500, 1024);

    @BeforeEach
    void setUp() {
        when(userService.getAll()).thenReturn(List.of(
                user(OLD_ID, "old", "admin", List.of("pokemon"), Instant.parse("2026-01-01T00:00:00Z")),
                user(NEW_ID, "new", "usuario", List.of("pokemon", "wow-forever"), Instant.parse("2026-06-01T00:00:00Z"))));
        when(catalog.getVisible(true, Set.of())).thenReturn(List.of(
                new GameResponse("pokemon", "Pokemon", null, "https://cdn/pokemon.png", false, true, 1),
                new GameResponse("wow-forever", "WoW Forever", null, null, false, true, 2)));
        when(providers.orderedStream()).thenAnswer(call -> Stream.empty());
    }

    @Test
    void theAccountsComeNewestFirstWithTheirLastSignIn() {
        when(authClient.adminListUsers()).thenReturn(new SupabaseAdminUsersResponse(List.of(
                new SupabaseAdminUsersResponse.Account(OLD_ID.toString(), SIGN_IN),
                new SupabaseAdminUsersResponse.Account(NEW_ID.toString(), null))));

        DashboardResponse.Users users = service.get().users();

        assertThat(users.accounts()).extracting(DashboardResponse.Account::username).containsExactly("new", "old");
        assertThat(users.accounts().getLast().lastActiveAt()).isEqualTo(SIGN_IN);
        assertThat(users.neverSignedIn()).isEqualTo(1);
        assertThat(users.admins()).isEqualTo(1);
        assertThat(users.players()).isEqualTo(1);
        assertThat(users.perGame()).containsExactly(new DashboardResponse.GameUsers("Pokemon", "https://cdn/pokemon.png", 2),
                new DashboardResponse.GameUsers("WoW Forever", null, 1));
    }

    @Test
    void theLastActivityIsTheLatestOfTheLastRequestAndTheLastSignIn() throws SQLException {
        Instant seen = SIGN_IN.plusSeconds(3600);
        ResultSet row = mock(ResultSet.class);
        when(row.getString("id")).thenReturn(OLD_ID.toString());
        when(row.getTimestamp("last_seen_at")).thenReturn(Timestamp.from(seen));
        doAnswer(call -> {
            call.<RowCallbackHandler>getArgument(1).processRow(row);

            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class));
        when(authClient.adminListUsers()).thenReturn(new SupabaseAdminUsersResponse(List.of(
                new SupabaseAdminUsersResponse.Account(OLD_ID.toString(), SIGN_IN))));

        assertThat(service.get().users().accounts().getLast().lastActiveAt()).isEqualTo(seen);
    }

    @Test
    void withoutSupabaseAuthTheSignInsAreUnknownNotNever() {
        when(authClient.adminListUsers()).thenThrow(new IllegalStateException("down"));

        DashboardResponse.Users users = service.get().users();

        assertThat(users.signInsKnown()).isFalse();
        assertThat(users.neverSignedIn()).isNull();
        assertThat(users.total()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void theStorageAddsUpItsBuckets() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1000L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(
                new DashboardResponse.Bucket("public-music", 3, 700), new DashboardResponse.Bucket("public-assets", 2, 300)));

        DashboardResponse.Storage storage = service.get().storage();

        assertThat(storage.databaseBytes()).isEqualTo(1000L);
        assertThat(storage.storageBytes()).isEqualTo(1000L);
        assertThat(storage.databaseLimitBytes()).isEqualTo(500L * 1024 * 1024);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aFailingQueryLeavesItsValueEmptyAndTheRestOfTheDashboardWorks() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenThrow(new IllegalStateException("denied"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenThrow(new IllegalStateException("denied"));

        DashboardResponse dashboard = service.get();

        assertThat(dashboard.storage().databaseBytes()).isNull();
        assertThat(dashboard.storage().storageBytes()).isNull();
        assertThat(dashboard.users().total()).isEqualTo(2);
        assertThat(dashboard.server().version()).isEqualTo("abcdef1");
    }

    @Test
    void aModuleThatFailsIsLeftOutAndTheOthersAreShown() {
        ModuleStatus status = new ModuleStatus("Game", List.of());
        ModuleStatusProvider broken = () -> {
            throw new IllegalStateException("broken");
        };
        when(providers.orderedStream()).thenAnswer(call -> Stream.of(broken, () -> status));

        assertThat(service.get().modules()).containsExactly(status);
    }

    @Test
    void theMemoryOfTheProcessComesFromItsStatus() {
        assertThat(DashboardService.residentBytes(List.of("Name: java", "VmRSS:\t  2048 kB", "Threads: 20")))
                .isEqualTo(2048L * 1024);
        assertThat(DashboardService.residentBytes(List.of("Name: java"))).isNull();
    }

    private static UserResponse user(UUID id, String name, String role, List<String> tags, Instant createdAt) {
        return new UserResponse(id, name + "@ntg.test", name, null, role, tags, createdAt);
    }
}
