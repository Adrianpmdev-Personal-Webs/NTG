package com.adrianpmdev.core.features.users.api.dto.response;

/**
 * Response DTO returned when an admin creates a user.
 *
 * @param user             created user
 * @param inviteEmailSent  false when the user was created but the email could not be sent (resend it later)
 *
 * @author adrianpmdev
 */
public record CreatedUserResponse(
    UserResponse user,
    boolean inviteEmailSent
) {}