package com.adrianpmdev.core.features.users.api.controller;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.adrianpmdev.core.features.users.api.dto.request.CreateUserRequest;
import com.adrianpmdev.core.features.users.api.dto.request.SetUserTagsRequest;
import com.adrianpmdev.core.features.users.api.dto.request.UpdateUserRequest;
import com.adrianpmdev.core.features.users.api.dto.response.CreatedUserResponse;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import com.adrianpmdev.core.features.users.application.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user operations. Everything is ADMIN-only except {@code PUT /me}
 * (see {@code SecurityConfig}). Users are addressed by id, never by email.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * Builds the controller with its dependencies.
     *
     * @param userService user service
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a regular user and emails them a link to choose their password.
     *
     * @param request email, username and optional tags
     * @param admin   authenticated admin
     * @return created user and whether the email was sent
     */
    @PostMapping
    public ResponseEntity<CreatedUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal AuthenticatedUser admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request, admin.id()));
    }

    /**
     * Lists all users with their tags.
     *
     * @return users
     */
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAll();
    }

    /**
     * Gets a user.
     *
     * @param id user id
     * @return user
     */
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getById(id);
    }

    /**
     * Updates the profile of the authenticated user.
     *
     * @param request new username and avatar
     * @param current authenticated user
     * @return updated user
     */
    @PutMapping("/me")
    public UserResponse updateMe(
            @Valid @RequestBody UpdateUserRequest request, @AuthenticationPrincipal AuthenticatedUser current) {
        return userService.updateProfile(current.id(), request, current.id());
    }

    /**
     * Updates the profile of any user.
     *
     * @param id      user id
     * @param request new username and avatar
     * @param admin   authenticated admin
     * @return updated user
     */
    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        return userService.updateProfile(id, request, admin.id());
    }

    /**
     * Deletes a user.
     *
     * @param id    user id
     * @param admin authenticated admin
     * @return empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser admin) {
        userService.delete(id, admin.id());

        return ResponseEntity.noContent().build();
    }

    /**
     * Sends the "choose your password" email again.
     *
     * @param id    user id
     * @param admin authenticated admin
     * @return empty response
     */
    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> resendInvite(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser admin) {
        userService.resendPasswordEmail(id, admin.id());

        return ResponseEntity.noContent().build();
    }

    /**
     * Replaces all tags of a user.
     *
     * @param id      user id
     * @param request tag keys
     * @param admin   authenticated admin
     * @return updated user
     */
    @PutMapping("/{id}/tags")
    public UserResponse setTags(
            @PathVariable UUID id, @Valid @RequestBody SetUserTagsRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        return userService.setTags(id, request.tags(), admin.id());
    }

    /**
     * Adds one tag to a user.
     *
     * @param id    user id
     * @param key   tag key
     * @param admin authenticated admin
     * @return updated user
     */
    @PostMapping("/{id}/tags/{key}")
    public UserResponse addTag(
            @PathVariable UUID id, @PathVariable String key, @AuthenticationPrincipal AuthenticatedUser admin) {
        return userService.addTag(id, key, admin.id());
    }

    /**
     * Removes one tag from a user.
     *
     * @param id    user id
     * @param key   tag key
     * @param admin authenticated admin
     * @return updated user
     */
    @DeleteMapping("/{id}/tags/{key}")
    public UserResponse removeTag(
            @PathVariable UUID id, @PathVariable String key, @AuthenticationPrincipal AuthenticatedUser admin) {
        return userService.removeTag(id, key, admin.id());
    }
}