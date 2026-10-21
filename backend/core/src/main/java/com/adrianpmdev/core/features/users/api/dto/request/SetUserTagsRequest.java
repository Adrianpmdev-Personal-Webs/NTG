package com.adrianpmdev.core.features.users.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO that replaces the whole tag set of a user.
 *
 * @param tags tag keys; an empty list removes all tags
 *
 * @author adrianpmdev
 */
public record SetUserTagsRequest(
    @NotNull(message = "Tags cannot be null")
    @Size(max = 50, message = "Too many tags")
    List<String> tags
) {}