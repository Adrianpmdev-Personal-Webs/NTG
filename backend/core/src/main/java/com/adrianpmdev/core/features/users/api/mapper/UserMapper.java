package com.adrianpmdev.core.features.users.api.mapper;

import com.adrianpmdev.core.features.tags.api.dto.response.TagResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;

import java.util.Comparator;

/**
 * Mapping utility for converting entities into response DTOs.
 * The user tags must be loaded (use a transactional service or an entity graph).
 *
 * @author adrianpmdev
 */
public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Converts a {@link User} entity into a {@link UserResponse}.
     *
     * @param user user entity with tags loaded
     * @return user response DTO
     */
    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getTags().stream().map(Tag::getKey).sorted(Comparator.naturalOrder()).toList(),
                user.getCreatedAt()
        );
    }

    /**
     * Converts a {@link Tag} entity into a {@link TagResponse}.
     *
     * @param tag tag entity
     * @return tag response DTO
     */
    public static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getKey(), tag.getName(), tag.getDescription(), tag.getCreatedAt());
    }
}