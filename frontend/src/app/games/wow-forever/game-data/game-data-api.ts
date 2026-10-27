/**
 * GameDataApi
 *
 * HTTP service for the game data of WoW Forever (talents, spellbooks, racials, the Legacy System and the URLs of the
 * pictures). Every screen reads its data through here: one request per screen (and per class), mapped by
 * GameDataMappers and kept for the rest of the session, so going back to a screen or a class does not download it again.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, shareReplay, throwError } from 'rxjs';
import { API_BASE_URL } from '@core/config/api';
import { LegacyData } from '../legacy/legacy-models';
import { RacialsData } from '../racials/racials-models';
import { ClassSpellbook } from '../spellbook/spellbook-models';
import { ClassTalents } from '../talents/talents-models';
import {
    DataIndexFile,
    LegacyResponseFile,
    RacialsResponseFile,
    SpellbookResponseFile,
    TalentsResponseFile
} from './game-data-files';
import { toClassSpellbook, toClassTalents, toGameIndex, toLegacy, toRacials } from './game-data-mappers';
import { GameIndex } from './game-data-models';

@Injectable({ providedIn: 'root' })
export class GameDataApi {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/wow-forever/data`;
    private readonly cache = new Map<string, Observable<unknown>>();

    /** Classes with their icons, and the credit of the data. */
    getIndex(): Observable<GameIndex> {
        return this.cached<DataIndexFile, GameIndex>('', toGameIndex);
    }

    getTalents(classSlug: string): Observable<ClassTalents> {
        return this.cached<TalentsResponseFile, ClassTalents>(`/talents/${encodeURIComponent(classSlug)}`, toClassTalents);
    }

    getSpellbook(classSlug: string): Observable<ClassSpellbook> {
        return this.cached<SpellbookResponseFile, ClassSpellbook>(`/spellbook/${encodeURIComponent(classSlug)}`, toClassSpellbook);
    }

    getRacials(): Observable<RacialsData> {
        return this.cached<RacialsResponseFile, RacialsData>('/racials', toRacials);
    }

    getLegacy(): Observable<LegacyData> {
        return this.cached<LegacyResponseFile, LegacyData>('/legacy', toLegacy);
    }

    /**
     * Public URLs of the pictures uploaded by an admin, by short key: `logo`, `backgrounds/alliance`,
     * `backgrounds/horde` and `talents/<class>-<tree>`. Pictures not uploaded yet are missing.
     */
    getImages(): Observable<Readonly<Record<string, string>>> {
        return this.cached<Record<string, string>, Readonly<Record<string, string>>>('/images', images => images);
    }

    /** One shared request per path. A failed one is forgotten, so the next attempt asks the server again. */
    private cached<Source, Model>(path: string, mapper: (file: Source) => Model): Observable<Model> {
        const known = this.cache.get(path) as Observable<Model> | undefined;

        if (known) {
            return known;
        }

        const request = this.http.get<Source>(`${this.baseUrl}${path}`).pipe(
            map(mapper),
            catchError(error => {
                this.cache.delete(path);
                return throwError(() => error);
            }),
            shareReplay({ bufferSize: 1, refCount: false })
        );

        this.cache.set(path, request);

        return request;
    }
}
