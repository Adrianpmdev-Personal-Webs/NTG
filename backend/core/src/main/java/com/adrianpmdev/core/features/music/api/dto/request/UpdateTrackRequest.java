package com.adrianpmdev.core.features.music.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for renaming a song.
 *
 * @param title new name
 *
 * @author adrianpmdev
 */
public record UpdateTrackRequest(
    @NotBlank(message = "El título no puede estar vacío")
    @Size(max = 100, message = "El título es demasiado largo")
    String title
) {}
