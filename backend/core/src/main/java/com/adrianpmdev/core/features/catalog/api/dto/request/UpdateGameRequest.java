package com.adrianpmdev.core.features.catalog.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a game of the catalog. The key cannot change.
 *
 * @param name      display name
 * @param upcoming  whether it is still coming soon
 * @param sortOrder position in the lists
 *
 * @author adrianpmdev
 */
public record UpdateGameRequest(
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 60, message = "Name is too long")
    String name,

    boolean upcoming,

    @Min(value = 0, message = "Order cannot be negative")
    @Max(value = 999, message = "Order is too large")
    int sortOrder
) {}
