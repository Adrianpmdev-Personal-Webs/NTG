package com.adrianpmdev.core.features.users.api.controller;

import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.features.users.application.service.UserAvatarService;
import com.adrianpmdev.core.features.users.application.service.UserService;
import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import com.adrianpmdev.core.security.ratelimit.AuthRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * What a signed-in user changes of their own account. Every route acts on the caller, so nobody can reach an account
 * that is not theirs.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class OwnAccountController {

    private final UserAvatarService userAvatarService;
    private final UserService userService;
    private final AuthRateLimiter rateLimiter;

    /**
     * Replaces the avatar of the caller with an uploaded picture.
     *
     * @param file    the picture
     * @param current authenticated user
     * @return updated user
     */
    @PutMapping("/avatar")
    public UserResponse uploadAvatar(@RequestPart("file") MultipartFile file,
                                     @AuthenticationPrincipal AuthenticatedUser current) {
        return userAvatarService.upload(current.id(), file);
    }

    /**
     * Removes the avatar of the caller.
     *
     * @param current authenticated user
     * @return updated user
     */
    @DeleteMapping("/avatar")
    public UserResponse deleteAvatar(@AuthenticationPrincipal AuthenticatedUser current) {
        return userAvatarService.delete(current.id());
    }

    /**
     * Emails the caller a link to choose a new password.
     *
     * @param current authenticated user
     */
    @PostMapping("/password-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendPasswordEmail(@AuthenticationPrincipal AuthenticatedUser current) {
        rateLimiter.checkPasswordEmail(current.id());
        userService.resendPasswordEmail(current.id(), current.id());
    }
}
