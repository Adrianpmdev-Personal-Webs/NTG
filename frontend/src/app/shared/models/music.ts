/**
 * Music models
 *
 * The playlist of an area (the general web or a game) as the backend gives it: its songs in playing order.
 *
 * @author @adrianpmdev
 */

export interface MusicTrack {
    id: string;
    title: string;
    url: string;
    durationSeconds: number;
    sizeBytes: number;
}

export interface MusicArea {
    area: string;
    name: string;
    declared: boolean;
    tracks: MusicTrack[];
}
