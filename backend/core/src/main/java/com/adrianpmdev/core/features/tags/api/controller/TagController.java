package com.adrianpmdev.core.features.tags.api.controller;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.adrianpmdev.core.features.tags.api.dto.request.CreateTagRequest;
import com.adrianpmdev.core.features.tags.api.dto.request.UpdateTagRequest;
import com.adrianpmdev.core.features.tags.api.dto.response.TagResponse;
import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import com.adrianpmdev.core.features.tags.application.service.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * REST controller for tag management (ADMIN only, see {@code SecurityConfig}).
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    /**
     * Builds the controller with its dependencies.
     *
     * @param tagService tag service
     */
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * Lists all tags.
     *
     * @return tags
     */
    @GetMapping
    public List<TagResponse> getAll() {
        return tagService.getAll();
    }

    /**
     * Creates a tag.
     *
     * @param request tag data
     * @param admin   authenticated admin
     * @return created tag
     */
    @PostMapping
    public ResponseEntity<TagResponse> create(
            @Valid @RequestBody CreateTagRequest request, @AuthenticationPrincipal AuthenticatedUser admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request, admin.id()));
    }

    /**
     * Updates the name and description of a tag.
     *
     * @param key     tag key
     * @param request new data
     * @param admin   authenticated admin
     * @return updated tag
     */
    @PutMapping("/{key}")
    public TagResponse update(
            @PathVariable String key, @Valid @RequestBody UpdateTagRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        return tagService.update(key, request, admin.id());
    }

    /**
     * Deletes a tag and removes it from every user.
     *
     * @param key   tag key
     * @param admin authenticated admin
     * @return empty response
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key, @AuthenticationPrincipal AuthenticatedUser admin) {
        tagService.delete(key, admin.id());

        return ResponseEntity.noContent().build();
    }
}