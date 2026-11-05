/**
 * CharactersApi
 *
 * HTTP service of the characters of the group. Every write answers with the whole updated list, so the screen just
 * replaces its copy with the response. What the armory says comes apart: in short for the characters of one player, or
 * the whole sheet of one character.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { CharacterRequest, CharacterSheet, CharacterSummary, CharactersData } from './characters-models';

@Injectable({ providedIn: 'root' })
export class CharactersApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/wow-forever/characters`;

    getAll(): Observable<CharactersData> {
        return this.http.get<CharactersData>(this.baseUrl);
    }

    summaries(ownerId: string): Observable<CharacterSummary[]> {
        return this.http.get<CharacterSummary[]>(`${this.baseUrl}/summaries`, { params: { owner: ownerId } });
    }

    sheet(id: string): Observable<CharacterSheet> {
        return this.http.get<CharacterSheet>(`${this.baseUrl}/${id}/armory`);
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
