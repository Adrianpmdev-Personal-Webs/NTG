package com.adrianpmdev.core.security.authentication;

import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The authority cache: one database query per user while cached, fresh values after an eviction.
 *
 * @author adrianpmdev
 */
class UserAuthorityCacheTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserAuthorityCache cache = new UserAuthorityCache(userRepository);
    private final UUID userId = UUID.randomUUID();

    @Test
    void repeatedRequestsQueryTheDatabaseOnce() {
        when(userRepository.findWithTagsById(userId)).thenReturn(Optional.of(user("usuario", "pokemon")));

        cache.get(userId);
        cache.get(userId);
        Optional<UserAuthorityCache.Authorities> authorities = cache.get(userId);

        verify(userRepository, times(1)).findWithTagsById(userId);
        assertThat(authorities).hasValueSatisfying(value -> {
            assertThat(value.role()).isEqualTo("usuario");
            assertThat(value.tagKeys()).containsExactly("pokemon");
        });
    }

    @Test
    void evictionLoadsTheNewTags() {
        when(userRepository.findWithTagsById(userId))
                .thenReturn(Optional.of(user("usuario", "pokemon")))
                .thenReturn(Optional.of(user("usuario", "pokemon", "wow-forever")));

        cache.get(userId);
        cache.evict(userId);

        assertThat(cache.get(userId)).hasValueSatisfying(
                value -> assertThat(value.tagKeys()).containsExactlyInAnyOrder("pokemon", "wow-forever"));
    }

    @Test
    void evictAllForgetsEveryUser() {
        when(userRepository.findWithTagsById(userId)).thenReturn(Optional.of(user("usuario", "pokemon")));

        cache.get(userId);
        cache.evictAll();
        cache.get(userId);

        verify(userRepository, times(2)).findWithTagsById(userId);
    }

    @Test
    void userWithoutProfileIsCachedAsEmpty() {
        when(userRepository.findWithTagsById(userId)).thenReturn(Optional.empty());

        assertThat(cache.get(userId)).isEmpty();
        assertThat(cache.get(userId)).isEmpty();
        verify(userRepository, times(1)).findWithTagsById(userId);
    }

    private User user(String role, String... tagKeys) {
        User user = new User();
        user.setId(userId);
        user.setRole(role);

        for (String key : tagKeys) {
            user.getTags().add(new Tag(key, key, null));
        }

        return user;
    }
}
