package com.adrianpmdev.core.features.music.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for the new order of the playlist of an area.
 *
 * @param ids every song of the area, in the new order
 *
 * @author adrianpmdev
 */
public record ReorderTracksRequest(
    @NotNull(message = "Falta el orden de las canciones")
    List<UUID> ids
) {}
