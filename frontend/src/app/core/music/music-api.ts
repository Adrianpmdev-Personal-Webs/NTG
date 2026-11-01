/**
 * MusicApi
 *
 * HTTP service for the background music: the playlists the current user can hear and, for admins, adding, renaming
 * and deleting songs. Songs arrive here already compressed (see the admin music screen).
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { MusicArea, MusicTrack } from '@shared/models/music';

@Injectable({ providedIn: 'root' })
export class MusicApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/music`;

    getAreas(): Observable<MusicArea[]> {
        return this.http.get<MusicArea[]>(this.baseUrl);
    }

    upload(area: string, song: Blob, title: string, durationSeconds: number): Observable<MusicTrack> {
        const body = new FormData();
        body.append('file', song, 'song.m4a');
        body.append('title', title);
        body.append('durationSeconds', String(durationSeconds));

        return this.http.post<MusicTrack>(`${this.baseUrl}/areas/${area}/tracks`, body);
    }

    rename(id: string, title: string): Observable<MusicTrack> {
        return this.http.put<MusicTrack>(`${this.baseUrl}/tracks/${id}`, { title });
    }

    delete(id: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/tracks/${id}`);
    }
}
