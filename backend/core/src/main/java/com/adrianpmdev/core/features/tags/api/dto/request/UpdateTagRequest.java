package com.adrianpmdev.core.features.tags.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a tag. The key cannot change.
 *
 * @param name        display name
 * @param description optional description
 *
 * @author adrianpmdev
 */
public record UpdateTagRequest(
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 60, message = "Name is too long")
    String name,

    @Size(max = 255, message = "Description is too long")
    String description
) {}