package com.adrianpmdev.core.features.auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for login authentication.
 * The password policy is intentionally not validated here: login must answer 401 for wrong credentials.
 *
 * @param email    user email
 * @param password user password
 *
 * @author adrianpmdev
 */
public record LoginRequest(
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email")
    String email,

    @NotBlank(message = "Password cannot be empty")
    String password
) {}