/**
 * MusicApi
 *
 * HTTP service for the background music: the playlists the current user can hear.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { MusicArea } from '@shared/models/music';

@Injectable({ providedIn: 'root' })
export class MusicApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/music`;

    getAreas(): Observable<MusicArea[]> {
        return this.http.get<MusicArea[]>(this.baseUrl);
    }
}
