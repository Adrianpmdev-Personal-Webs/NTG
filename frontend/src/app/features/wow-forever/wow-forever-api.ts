/**
 * WowForeverApi
 *
 * HTTP service for the WoW Forever base data. Reading needs the module tag; writing is for admins.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/config/api';
import {
    Race,
    RaceCreateRequest,
    RaceUpdateRequest,
    WowClass,
    WowClassCreateRequest,
    WowClassUpdateRequest,
    WowSetting
} from './wow-forever-models';

@Injectable({ providedIn: 'root' })
export class WowForeverApi {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/wow-forever`;

    getClasses(): Observable<WowClass[]> {
        return this.http.get<WowClass[]>(`${this.baseUrl}/classes`);
    }

    createClass(request: WowClassCreateRequest): Observable<WowClass> {
        return this.http.post<WowClass>(`${this.baseUrl}/classes`, request);
    }

    updateClass(id: string, request: WowClassUpdateRequest): Observable<WowClass> {
        return this.http.put<WowClass>(`${this.baseUrl}/classes/${id}`, request);
    }

    deleteClass(id: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/classes/${id}`);
    }

    getRaces(): Observable<Race[]> {
        return this.http.get<Race[]>(`${this.baseUrl}/races`);
    }

    createRace(request: RaceCreateRequest): Observable<Race> {
        return this.http.post<Race>(`${this.baseUrl}/races`, request);
    }

    updateRace(id: string, request: RaceUpdateRequest): Observable<Race> {
        return this.http.put<Race>(`${this.baseUrl}/races/${id}`, request);
    }

    deleteRace(id: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/races/${id}`);
    }

    getSettings(): Observable<WowSetting[]> {
        return this.http.get<WowSetting[]>(`${this.baseUrl}/settings`);
    }

    updateSetting(key: string, value: string): Observable<WowSetting> {
        return this.http.put<WowSetting>(`${this.baseUrl}/settings/${key}`, { value });
    }
}
