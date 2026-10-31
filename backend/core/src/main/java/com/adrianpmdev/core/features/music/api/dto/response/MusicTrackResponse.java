package com.adrianpmdev.core.features.music.api.dto.response;

import java.util.UUID;

/**
 * A song of a playlist.
 *
 * @param id              song id
 * @param title           name shown to the admin
 * @param url             public URL of the file (it never changes, so browsers keep it)
 * @param durationSeconds length of the song
 * @param sizeBytes       file size
 *
 * @author adrianpmdev
 */
public record MusicTrackResponse(
    UUID id,
    String title,
    String url,
    int durationSeconds,
    int sizeBytes
) {}
