package com.adrianpmdev.core.security.authentication;

import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Role and tags of each user, kept in memory for {@value #TTL_SECONDS} seconds so the access token filter does not
 * query the database on every request (one query per user per minute at most, instead of one per request).
 * <p>
 * Changes still apply right away: whatever changes a role or the tags of a user (or deletes it) evicts that user, and
 * deleting a tag evicts everybody. Evictions are repeated after the transaction commits, so a request running at the
 * same time cannot put the old values back. The time limit only bounds what a missed eviction could cost.
 *
 * @author adrianpmdev
 */
@Component
public class UserAuthorityCache {

    static final long TTL_SECONDS = 60;

    private static final long MAX_USERS = 1_000;

    /**
     * What the security layer needs from a user.
     *
     * @param role    stored role (not normalized)
     * @param tagKeys keys of its tags
     */
    public record Authorities(String role, Set<String> tagKeys) {
    }

    private final UserRepository userRepository;

    /** An empty value means "valid token, but no local profile", which is cached too. */
    private final Cache<UUID, Optional<Authorities>> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(TTL_SECONDS))
            .maximumSize(MAX_USERS)
            .build();

    /**
     * Builds the cache.
     *
     * @param userRepository local user repository
     */
    public UserAuthorityCache(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Role and tags of a user, from memory or from the database.
     *
     * @param userId user id
     * @return its authorities, or empty when the user has no local profile
     */
    public Optional<Authorities> get(UUID userId) {
        return cache.get(userId, this::load);
    }

    /**
     * Forgets a user (call it when its role or tags change, or when it is deleted).
     *
     * @param userId user id
     */
    public void evict(UUID userId) {
        cache.invalidate(userId);
        afterCommit(() -> cache.invalidate(userId));
    }

    /** Forgets every user (call it when a tag is deleted). */
    public void evictAll() {
        cache.invalidateAll();
        afterCommit(cache::invalidateAll);
    }

    private Optional<Authorities> load(UUID userId) {
        return userRepository.findWithTagsById(userId)
                .map(user -> new Authorities(user.getRole(),
                        user.getTags().stream().map(Tag::getKey).collect(Collectors.toUnmodifiableSet())));
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        }
    }
}
