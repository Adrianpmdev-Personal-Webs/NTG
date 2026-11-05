package com.adrianpmdev.wowforever.features.characters.infrastructure.armory;

import java.time.Instant;

/**
 * A character found by the search of the armory.
 *
 * @param slug        its id in the armory
 * @param lastUpdated when the armory last changed it, or null when it does not tell
 *
 * @author adrianpmdev
 */
public record ArmoryMatch(String slug, Instant lastUpdated) {
}
