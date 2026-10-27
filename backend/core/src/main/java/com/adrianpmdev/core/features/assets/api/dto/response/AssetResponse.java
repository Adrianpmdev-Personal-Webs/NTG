package com.adrianpmdev.core.features.assets.api.dto.response;

import java.time.Instant;

/**
 * A picture slot for the admin screen: what it is for and, when it has been uploaded, its file.
 *
 * @param key         picture key
 * @param group       heading of the group it belongs to
 * @param label       name of the picture
 * @param declared    false for an uploaded picture that no module expects any more (it can be deleted)
 * @param url         public URL, or null when it has not been uploaded
 * @param contentType MIME type, or null
 * @param sizeBytes   file size, or null
 * @param width       width in pixels, or null
 * @param height      height in pixels, or null
 * @param updatedAt   last upload, or null
 *
 * @author adrianpmdev
 */
public record AssetResponse(
    String key,
    String group,
    String label,
    boolean declared,
    String url,
    String contentType,
    Integer sizeBytes,
    Integer width,
    Integer height,
    Instant updatedAt
) {}
