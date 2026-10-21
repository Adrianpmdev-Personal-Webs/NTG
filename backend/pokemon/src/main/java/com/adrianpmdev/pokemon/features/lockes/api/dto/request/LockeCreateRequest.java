package com.adrianpmdev.pokemon.features.lockes.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating lockes.
 *
 * @author adrianpmdev
 */
public record LockeCreateRequest(
        @NotNull
        UUID gameId,

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        Object rules
) {}