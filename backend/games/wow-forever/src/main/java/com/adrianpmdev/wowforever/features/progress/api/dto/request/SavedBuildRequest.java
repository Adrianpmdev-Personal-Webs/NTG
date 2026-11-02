package com.adrianpmdev.wowforever.features.progress.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A talent build to keep.
 *
 * @param name  name the player gives it
 * @param level level of the build
 * @param build share code of the build
 *
 * @author adrianpmdev
 */
public record SavedBuildRequest(
        @NotBlank @Size(max = 30) String name,
        @Min(1) @Max(200) int level,
        @NotBlank @Size(max = 300) @Pattern(regexp = "[0-9a-zA-Z.-]+") String build
) {
}
