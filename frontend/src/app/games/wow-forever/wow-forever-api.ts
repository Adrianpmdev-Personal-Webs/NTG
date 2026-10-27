/**
 * WowForeverApi
 *
 * HTTP service for the WoW Forever module settings. Reading needs the module tag; writing is for admins. The game
 * data (talents, spellbooks, racials, Legacy) has its own service, GameDataApi.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/config/api';
import { WowSetting } from './wow-forever-models';

@Injectable({ providedIn: 'root' })
export class WowForeverApi {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/wow-forever`;

    getSettings(): Observable<WowSetting[]> {
        return this.http.get<WowSetting[]>(`${this.baseUrl}/settings`);
    }

    updateSetting(key: string, value: string): Observable<WowSetting> {
        return this.http.put<WowSetting>(`${this.baseUrl}/settings/${key}`, { value });
    }
}
