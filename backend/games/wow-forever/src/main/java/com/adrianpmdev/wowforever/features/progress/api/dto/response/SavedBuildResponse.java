package com.adrianpmdev.wowforever.features.progress.api.dto.response;

/**
 * A talent build kept by a player.
 *
 * @param name  name the player gave it
 * @param level level of the build
 * @param build share code of the build
 * @param order order the points were spent in, null when the build was kept without it
 *
 * @author adrianpmdev
 */
public record SavedBuildResponse(String name, int level, String build, String order) {
}
