package com.adrianpmdev.core.features.catalog.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a game to the catalog.
 *
 * @param key       lowercase key (letters, digits, '-' or '_'), the same as the tag that will unlock it
 * @param name      display name
 * @param upcoming  whether it is still coming soon
 * @param sortOrder position in the lists
 *
 * @author adrianpmdev
 */
public record CreateGameRequest(
    @NotBlank(message = "Key cannot be empty")
    @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,29}$", message = "Key must be 2-30 lowercase letters, digits, '-' or '_'")
    String key,

    @NotBlank(message = "Name cannot be empty")
    @Size(max = 60, message = "Name is too long")
    String name,

    boolean upcoming,

    @Min(value = 0, message = "Order cannot be negative")
    @Max(value = 999, message = "Order is too large")
    int sortOrder
) {}
