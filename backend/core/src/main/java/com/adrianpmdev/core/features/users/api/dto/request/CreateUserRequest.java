package com.adrianpmdev.core.features.users.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a regular user. The user chooses the password from the email link.
 *
 * @param email    user email
 * @param username unique display name
 * @param tags     optional tag keys to assign right away
 *
 * @author adrianpmdev
 */
public record CreateUserRequest(
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email")
    @Size(max = 320, message = "Email is too long")
    String email,

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must have between 3 and 50 characters")
    String username,

    @Size(max = 50, message = "Too many tags")
    List<@NotBlank String> tags
) {}