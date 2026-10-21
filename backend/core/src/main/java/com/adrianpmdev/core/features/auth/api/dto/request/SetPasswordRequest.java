package com.adrianpmdev.core.features.auth.api.dto.request;

import com.adrianpmdev.core.features.auth.api.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for choosing a password from the Supabase email link.
 *
 * @param token    access token received in the email link (URL fragment {@code access_token})
 * @param password new password chosen by the user
 *
 * @author adrianpmdev
 */
public record SetPasswordRequest(
    @NotBlank(message = "Token cannot be empty")
    String token,

    @ValidPassword
    String password
) {}