package com.adrianpmdev.core.features.catalog.api.dto.response;

/**
 * A game of the catalog as the caller sees it.
 *
 * @param id         game id (tag key and URL segment)
 * @param name       display name
 * @param coverUrl   public URL of the cover, or null when none has been uploaded
 * @param upcoming   whether it is still coming soon
 * @param accessible whether the caller can open it (admins always; players with the tag once it is released)
 * @param sortOrder  position in the lists
 *
 * @author adrianpmdev
 */
public record GameResponse(
    String id,
    String name,
    String coverUrl,
    boolean upcoming,
    boolean accessible,
    int sortOrder
) {}
