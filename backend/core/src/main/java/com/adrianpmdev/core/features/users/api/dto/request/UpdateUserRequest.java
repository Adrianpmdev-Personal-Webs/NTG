package com.adrianpmdev.core.features.users.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a user profile.
 *
 * @param username  user's display name
 * @param avatarUrl optional http(s) avatar URL
 *
 * @author adrianpmdev
 */
public record UpdateUserRequest(
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must have between 3 and 50 characters")
    String username,

    @Size(max = 500, message = "Avatar URL is too long")
    @Pattern(regexp = "^(https?://.+)?$", message = "Avatar URL must start with http:// or https://")
    String avatarUrl
) {}