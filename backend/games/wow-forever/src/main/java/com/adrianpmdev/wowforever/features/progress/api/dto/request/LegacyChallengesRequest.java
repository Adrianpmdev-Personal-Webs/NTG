package com.adrianpmdev.wowforever.features.progress.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Legacy challenges to tick or untick.
 *
 * @param names     names of the challenges
 * @param completed true to tick them, false to untick them
 *
 * @author adrianpmdev
 */
public record LegacyChallengesRequest(
        @NotEmpty @Size(max = 500) List<@NotBlank @Size(max = 200) String> names,
        boolean completed
) {
}
