package com.adrianpmdev.platform.image;

/**
 * Limits of a stored picture.
 *
 * @param maxSide  longest side in pixels; larger pictures are scaled down, smaller ones are kept as they are
 * @param maxBytes largest size of the stored file
 *
 * @author adrianpmdev
 */
public record ImageSpec(int maxSide, int maxBytes) {

    /**
     * Validates the limits.
     */
    public ImageSpec {
        if (maxSide <= 0 || maxBytes <= 0) {
            throw new IllegalArgumentException("Image limits must be positive");
        }
    }
}
