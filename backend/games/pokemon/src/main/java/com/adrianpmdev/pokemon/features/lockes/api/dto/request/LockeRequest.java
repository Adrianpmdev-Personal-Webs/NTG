package com.adrianpmdev.pokemon.features.lockes.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data of a randomlocke, to create it or to replace it.
 *
 * @param name        randomlocke name
 * @param description optional description
 * @param rules       rules document (any JSON, see the frontend LockeRules model), or null
 *
 * @author adrianpmdev
 */
public record LockeRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        Object rules
) {
}
