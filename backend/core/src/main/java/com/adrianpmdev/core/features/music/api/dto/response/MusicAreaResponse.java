package com.adrianpmdev.core.features.music.api.dto.response;

import java.util.List;

/**
 * The playlist of an area: the platform ({@code platform}) or a game (its id).
 *
 * @param area     area id, also the folder of its songs in the bucket
 * @param name     name of the area
 * @param declared false for songs of an area no module declares any more (admins only, so they can delete them)
 * @param tracks   songs in playing order (empty: the area plays nothing)
 *
 * @author adrianpmdev
 */
public record MusicAreaResponse(
    String area,
    String name,
    boolean declared,
    List<MusicTrackResponse> tracks
) {}
