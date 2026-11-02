/**
 * PlayerProgressApi
 *
 * What the player keeps on their account in WoW Forever: the talent builds of each class and the ticked Legacy challenges.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { SavedBuild, SavedBuildSlots } from '../talents/saved-builds-models';

@Injectable({ providedIn: 'root' })
export class PlayerProgressApi {
    private readonly http = inject(HttpClient);
    private readonly base = `${API_BASE_URL}/wow-forever/me`;

    loadBuilds(classSlug: string): Observable<SavedBuildSlots> {
        return this.http.get<SavedBuildSlots>(`${this.base}/builds/${encodeURIComponent(classSlug)}`);
    }

    saveBuild(classSlug: string, slot: number, build: SavedBuild): Observable<SavedBuildSlots> {
        return this.http.put<SavedBuildSlots>(`${this.base}/builds/${encodeURIComponent(classSlug)}/${slot}`, build);
    }

    deleteBuild(classSlug: string, slot: number): Observable<SavedBuildSlots> {
        return this.http.delete<SavedBuildSlots>(`${this.base}/builds/${encodeURIComponent(classSlug)}/${slot}`);
    }

    loadChallenges(): Observable<ReadonlySet<string>> {
        return this.http.get<string[]>(`${this.base}/legacy-challenges`).pipe(map((names) => new Set(names)));
    }

    setChallenge(name: string, completed: boolean): Observable<ReadonlySet<string>> {
        return this.http
            .put<string[]>(`${this.base}/legacy-challenges`, { names: [name], completed })
            .pipe(map((names) => new Set(names)));
    }
}
