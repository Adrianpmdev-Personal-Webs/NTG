package com.adrianpmdev.platform.image;

/**
 * A picture ready to be stored.
 *
 * @param bytes       file content
 * @param contentType MIME type ({@code image/jpeg} or {@code image/png})
 * @param extension   file extension without the dot ({@code jpg} or {@code png})
 * @param width       width in pixels
 * @param height      height in pixels
 *
 * @author adrianpmdev
 */
public record CompressedImage(byte[] bytes, String contentType, String extension, int width, int height) {
}
