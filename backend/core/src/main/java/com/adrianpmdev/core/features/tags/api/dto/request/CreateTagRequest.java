package com.adrianpmdev.core.features.tags.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a tag.
 *
 * @param key         lowercase key (letters, digits, '-' or '_'), e.g. "pokemon"
 * @param name        display name
 * @param description optional description
 *
 * @author adrianpmdev
 */
public record CreateTagRequest(
    @NotBlank(message = "Key cannot be empty")
    @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,29}$",
            message = "Key must be 2-30 characters: lowercase letters, digits, '-' or '_'")
    String key,

    @NotBlank(message = "Name cannot be empty")
    @Size(max = 60, message = "Name is too long")
    String name,

    @Size(max = 255, message = "Description is too long")
    String description
) {}