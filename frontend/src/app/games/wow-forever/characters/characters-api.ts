/**
 * CharactersApi
 *
 * HTTP service of the characters of the group. Every write answers with the whole updated list, so the screen just
 * replaces its copy with the response.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { CharacterRequest, CharactersData } from './characters-models';

@Injectable({ providedIn: 'root' })
export class CharactersApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/wow-forever/characters`;

    getAll(): Observable<CharactersData> {
        return this.http.get<CharactersData>(this.baseUrl);
    }

    refresh(): Observable<CharactersData> {
        return this.http.post<CharactersData>(`${this.baseUrl}/refresh`, null);
    }

    create(request: CharacterRequest): Observable<CharactersData> {
        return this.http.post<CharactersData>(this.baseUrl, request);
    }

    update(id: string, request: CharacterRequest): Observable<CharactersData> {
        return this.http.put<CharactersData>(`${this.baseUrl}/${id}`, request);
    }

    delete(id: string): Observable<CharactersData> {
        return this.http.delete<CharactersData>(`${this.baseUrl}/${id}`);
    }
}
