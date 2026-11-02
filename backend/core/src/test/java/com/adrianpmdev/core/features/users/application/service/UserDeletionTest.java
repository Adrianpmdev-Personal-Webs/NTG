package com.adrianpmdev.core.features.users.application.service;

import com.adrianpmdev.core.config.AppProperties;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.repository.TagRepository;
import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import com.adrianpmdev.core.integration.supabase.auth.client.SupabaseAuthClient;
import com.adrianpmdev.core.security.authentication.UserAuthorityCache;
import com.adrianpmdev.platform.module.UserDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that deleting an account asks every game to clean first, and keeps the account when a game fails.
 *
 * @author adrianpmdev
 */
class UserDeletionTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final String AVATAR = "https://storage/avatar.png";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SupabaseAuthClient supabase = mock(SupabaseAuthClient.class);
    private final UserAvatarService avatars = mock(UserAvatarService.class);
    private final UserDataCleaner first = mock(UserDataCleaner.class);
    private final UserDataCleaner second = mock(UserDataCleaner.class);

    private UserService service;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setAvatarUrl(AVATAR);
        when(userRepository.findById(USER)).thenReturn(Optional.of(user));

        service = new UserService(userRepository, mock(TagRepository.class), supabase, mock(AppProperties.class),
                mock(UserAuthorityCache.class), avatars, List.of(first, second));
    }

    @Test
    void cleansEveryGameBeforeDeletingTheAccount() {
        service.delete(USER, ADMIN);

        InOrder order = inOrder(first, second, supabase, userRepository, avatars);
        order.verify(first).removeUser(USER);
        order.verify(second).removeUser(USER);
        order.verify(supabase).adminDeleteUser(USER);
        order.verify(userRepository).deleteById(USER);
        order.verify(avatars).deleteReplacedUpload(USER, AVATAR, null);
    }

    @Test
    void keepsTheAccountWhenAGameFails() {
        doThrow(new IllegalStateException("database down")).when(second).removeUser(USER);

        assertThatThrownBy(() -> service.delete(USER, ADMIN)).isInstanceOf(IllegalStateException.class);

        verify(supabase, never()).adminDeleteUser(USER);
        verify(userRepository, never()).deleteById(USER);
    }

    @Test
    void nobodyDeletesTheirOwnAccount() {
        assertThatThrownBy(() -> service.delete(ADMIN, ADMIN));

        verify(first, never()).removeUser(ADMIN);
        verify(supabase, never()).adminDeleteUser(ADMIN);
    }
}
