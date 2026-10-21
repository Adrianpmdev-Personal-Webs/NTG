package com.adrianpmdev.core.features.users.application.service;

import com.adrianpmdev.core.config.AppProperties;
import com.adrianpmdev.core.features.users.api.dto.request.CreateUserRequest;
import com.adrianpmdev.core.features.users.api.dto.request.UpdateUserRequest;
import com.adrianpmdev.core.features.users.api.dto.response.CreatedUserResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.features.tags.application.service.TagService;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import com.adrianpmdev.core.features.users.api.mapper.UserMapper;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.repository.TagRepository;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import com.adrianpmdev.core.integration.supabase.auth.client.SupabaseAuthClient;
import com.adrianpmdev.core.integration.supabase.auth.exception.SupabaseAuthException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing users: Supabase Auth account, local profile and tags.
 * Technical events go to the normal log; admin actions also go to the {@code AUDIT} logger.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
public class UserService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final SupabaseAuthClient supabase;
    private final AppProperties appProperties;

    /**
     * Builds the service with its dependencies.
     *
     * @param userRepository user repository
     * @param tagRepository  tag repository
     * @param supabase       Supabase Auth client
     * @param appProperties  application settings
     */
    public UserService(UserRepository userRepository, TagRepository tagRepository,
                       SupabaseAuthClient supabase, AppProperties appProperties) {
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.supabase = supabase;
        this.appProperties = appProperties;
    }

    /**
     * Creates a regular user and emails them a link to choose their own password.
     * The Supabase account is created first (confirmed, without password); if saving the local profile fails
     * the Supabase account is deleted again. A failure sending the email does not undo the creation:
     * the response says so and the admin can resend it.
     *
     * @param request user data
     * @param actorId admin performing the action
     * @return created user and whether the email could be sent
     */
    public CreatedUserResponse create(CreateUserRequest request, UUID actorId) {
        String email = normalizeEmail(request.email());
        String username = request.username().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + username + "' is already in use");
        }

        Set<Tag> tags = resolveTags(request.tags());
        UUID userId = createInSupabase(email);
        User saved = saveLocalProfile(userId, email, username, tags);

        boolean inviteSent = sendPasswordEmail(email);
        AUDIT.info("actor={} action=USER_CREATED target={} tags={} inviteSent={}",
                actorId, userId, tagKeys(tags), inviteSent);

        return new CreatedUserResponse(UserMapper.toResponse(saved), inviteSent);
    }

    /**
     * Lists all users.
     *
     * @return users ordered by username
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAllByOrderByUsernameAsc().stream().map(UserMapper::toResponse).toList();
    }

    /**
     * Gets a user by id.
     *
     * @param id user id
     * @return user
     */
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserMapper.toResponse(find(id));
    }

    /**
     * Updates the profile (username and avatar) of a user.
     *
     * @param id      user to update
     * @param request new data
     * @param actorId user performing the action (the same user or an admin)
     * @return updated user
     */
    @Transactional
    public UserResponse updateProfile(UUID id, UpdateUserRequest request, UUID actorId) {
        User user = find(id);
        String username = request.username().trim();

        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + username + "' is already in use");
        }

        user.setUsername(username);
        user.setAvatarUrl(request.avatarUrl() == null || request.avatarUrl().isBlank() ? null : request.avatarUrl());
        AUDIT.info("actor={} action=USER_UPDATED target={}", actorId, id);

        return UserMapper.toResponse(user);
    }

    /**
     * Deletes a user from Supabase Auth first and then from the local schema.
     *
     * @param id      user to delete
     * @param actorId admin performing the action
     */
    public void delete(UUID id, UUID actorId) {
        if (id.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }

        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE);
        }

        try {
            supabase.adminDeleteUser(id);
        } catch (SupabaseAuthException exception) {
            log.error("Could not delete Supabase user {}", id, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not delete the account", exception);
        }

        userRepository.deleteById(id);
        AUDIT.info("actor={} action=USER_DELETED target={}", actorId, id);
    }

    /**
     * Sends the password email again (invite expired, lost, or the user forgot the password).
     *
     * @param id      target user
     * @param actorId admin performing the action
     */
    public void resendPasswordEmail(UUID id, UUID actorId) {
        User user = find(id);

        try {
            supabase.sendPasswordEmail(user.getEmail(), appProperties.setPasswordUrl());
        } catch (SupabaseAuthException exception) {
            log.warn("Could not send password email to {} (status {}): {}", id, exception.status(), exception.getMessage());
            throw exception.status() == 429
                    ? new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Email rate limit reached, try again later", exception)
                    : new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send the email", exception);
        }

        AUDIT.info("actor={} action=PASSWORD_EMAIL_SENT target={}", actorId, id);
    }

    /**
     * Replaces the whole tag set of a user.
     *
     * @param id      target user
     * @param keys    tag keys, empty to remove all
     * @param actorId admin performing the action
     * @return updated user
     */
    @Transactional
    public UserResponse setTags(UUID id, Collection<String> keys, UUID actorId) {
        User user = find(id);
        Set<Tag> tags = resolveTags(keys);
        user.getTags().clear();
        user.getTags().addAll(tags);
        AUDIT.info("actor={} action=USER_TAGS_SET target={} tags={}", actorId, id, tagKeys(tags));

        return UserMapper.toResponse(user);
    }

    /**
     * Adds one tag to a user.
     *
     * @param id      target user
     * @param key     tag key
     * @param actorId admin performing the action
     * @return updated user
     */
    @Transactional
    public UserResponse addTag(UUID id, String key, UUID actorId) {
        User user = find(id);
        Tag tag = tagRepository.findById(TagService.normalizeKey(key))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
        user.getTags().add(tag);
        AUDIT.info("actor={} action=USER_TAG_ADDED target={} tag={}", actorId, id, tag.getKey());

        return UserMapper.toResponse(user);
    }

    /**
     * Removes one tag from a user. Removing a tag the user does not have is not an error.
     *
     * @param id      target user
     * @param key     tag key
     * @param actorId admin performing the action
     * @return updated user
     */
    @Transactional
    public UserResponse removeTag(UUID id, String key, UUID actorId) {
        User user = find(id);
        String normalized = TagService.normalizeKey(key);
        user.getTags().removeIf(tag -> tag.getKey().equals(normalized));
        AUDIT.info("actor={} action=USER_TAG_REMOVED target={} tag={}", actorId, id, normalized);

        return UserMapper.toResponse(user);
    }

    private User find(UUID id) {
        return userRepository.findWithTagsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE));
    }

    private Set<Tag> resolveTags(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> normalized = new HashSet<>();
        keys.forEach(key -> normalized.add(TagService.normalizeKey(key)));

        List<Tag> found = tagRepository.findAllById(normalized);

        if (found.size() != normalized.size()) {
            Set<String> missing = new HashSet<>(normalized);
            found.forEach(tag -> missing.remove(tag.getKey()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tags: " + missing);
        }

        return new HashSet<>(found);
    }

    private UUID createInSupabase(String email) {
        try {
            return supabase.adminCreateUser(email);
        } catch (SupabaseAuthException exception) {
            if ("email_exists".equals(exception.errorCode()) || exception.status() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered", exception);
            }

            log.error("Supabase refused to create a user (status {}): {}", exception.status(), exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create the account", exception);
        }
    }

    private User saveLocalProfile(UUID userId, String email, String username, Set<Tag> tags) {
        try {
            User user = User.newRegularUser(userId, email, username);
            user.getTags().addAll(tags);

            return userRepository.saveAndFlush(user);
        } catch (RuntimeException exception) {
            log.error("Failed to save local profile for {}; removing the Supabase account", userId, exception);
            compensateOrphanUser(userId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error saving the user", exception);
        }
    }

    private void compensateOrphanUser(UUID userId) {
        try {
            supabase.adminDeleteUser(userId);
        } catch (RuntimeException exception) {
            log.error("ORPHAN Supabase user {}: compensation failed, delete it manually", userId, exception);
        }
    }

    private boolean sendPasswordEmail(String email) {
        try {
            supabase.sendPasswordEmail(email, appProperties.setPasswordUrl());

            return true;
        } catch (SupabaseAuthException exception) {
            log.warn("User created but the password email failed (status {}): {}", exception.status(), exception.getMessage());

            return false;
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> tagKeys(Collection<Tag> tags) {
        return tags.stream().map(Tag::getKey).sorted().toList();
    }
}