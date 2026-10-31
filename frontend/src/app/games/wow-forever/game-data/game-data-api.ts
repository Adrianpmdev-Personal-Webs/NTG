/**
 * GameDataApi
 *
 * HTTP service for the game data of WoW Forever (talents, spellbooks, racials, the Legacy System, professions and the
 * URLs of the pictures). The backend sends every screen its data already in the models of the app (texts in Spanish, full icon
 * URLs), so this service only downloads it: one request per screen (and per class), kept for the rest of the session,
 * so going back to a screen or a class does not download it again.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, shareReplay, throwError } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { LegacyData } from '../legacy/legacy-models';
import { ProfessionsData } from '../professions/professions-models';
import { RacialsData } from '../racials/racials-models';
import { ClassSpellbook } from '../spellbook/spellbook-models';
import { ClassTalents } from '../talents/talents-models';
import { GameIndex } from './game-data-models';

@Injectable({ providedIn: 'root' })
export class GameDataApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/wow-forever/data`;
    private readonly cache = new Map<string, Observable<unknown>>();

    getIndex(): Observable<GameIndex> {
        return this.cached<GameIndex>('');
    }

    getTalents(classSlug: string): Observable<ClassTalents> {
        return this.cached<ClassTalents>(`/talents/${encodeURIComponent(classSlug)}`);
    }

    getSpellbook(classSlug: string): Observable<ClassSpellbook> {
        return this.cached<ClassSpellbook>(`/spellbook/${encodeURIComponent(classSlug)}`);
    }

    getRacials(): Observable<RacialsData> {
        return this.cached<RacialsData>('/racials');
    }

    getLegacy(): Observable<LegacyData> {
        return this.cached<LegacyData>('/legacy');
    }

    getProfessions(): Observable<ProfessionsData> {
        return this.cached<ProfessionsData>('/professions');
    }

    getImages(): Observable<Readonly<Record<string, string>>> {
        return this.cached<Readonly<Record<string, string>>>('/images');
    }

    private cached<Model>(path: string): Observable<Model> {
        const known = this.cache.get(path) as Observable<Model> | undefined;

        if (known) {
            return known;
        }

        const request = this.http.get<Model>(`${this.baseUrl}${path}`).pipe(
            catchError((error) => {
                this.cache.delete(path);
                return throwError(() => error);
            }),
            shareReplay({ bufferSize: 1, refCount: false }),
        );

        this.cache.set(path, request);

        return request;
    }
}
