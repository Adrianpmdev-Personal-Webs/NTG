package com.adrianpmdev.platform.image;

/**
 * Turns a picture uploaded by a client into the file that gets stored. Implemented by core, so every module (and
 * every web client) relies on one single place that validates and compresses pictures:
 * <ul>
 *     <li>the content is checked by its first bytes, never by the file name or the declared type;</li>
 *     <li>the picture is resized to fit {@link ImageSpec#maxSide()} and re-encoded, which also drops its metadata
 *     (camera, GPS position...);</li>
 *     <li>pictures with transparency become PNG, the rest JPEG, and the quality is lowered (or the size reduced)
 *     until the file fits {@link ImageSpec#maxBytes()}.</li>
 * </ul>
 * Invalid pictures end in a client error (400, 413 or 415) with a message for the user.
 *
 * @author adrianpmdev
 */
public interface ImageCompressor {

    /**
     * Validates, resizes and re-encodes a picture.
     *
     * @param source bytes received from the client
     * @param spec   limits of the stored file
     * @return the compressed picture
     */
    CompressedImage compress(byte[] source, ImageSpec spec);
}
