/**
 * LolApi
 *
 * HTTP service of the League of Legends module: one method per endpoint. It never calls Riot: the backend does, and the
 * key stays there.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { LolAdminAccount, LolPlayerDetail, LolPlayers } from './lol-models';

@Injectable({ providedIn: 'root' })
export class LolApi {
    private readonly http = inject(HttpClient);
    private readonly base = `${API_BASE_URL}/lol`;

    getPlayers(): Observable<LolPlayers> {
        return this.http.get<LolPlayers>(`${this.base}/players`);
    }

    getPlayer(userId: string): Observable<LolPlayerDetail> {
        return this.http.get<LolPlayerDetail>(`${this.base}/players/${userId}`);
    }

    refresh(userId: string): Observable<LolPlayerDetail> {
        return this.http.post<LolPlayerDetail>(`${this.base}/players/${userId}/refresh`, {});
    }

    linkOwn(riotId: string): Observable<LolPlayers> {
        return this.http.put<LolPlayers>(`${this.base}/accounts/me`, { riotId });
    }

    unlinkOwn(): Observable<LolPlayers> {
        return this.http.delete<LolPlayers>(`${this.base}/accounts/me`);
    }

    getAccounts(): Observable<LolAdminAccount[]> {
        return this.http.get<LolAdminAccount[]>(`${this.base}/admin/accounts`);
    }

    linkAccount(userId: string, riotId: string): Observable<LolAdminAccount[]> {
        return this.http.put<LolAdminAccount[]>(`${this.base}/admin/accounts/${userId}`, { riotId });
    }

    unlinkAccount(userId: string): Observable<LolAdminAccount[]> {
        return this.http.delete<LolAdminAccount[]>(`${this.base}/admin/accounts/${userId}`);
    }

    download(userId: string): Observable<LolAdminAccount[]> {
        return this.http.post<LolAdminAccount[]>(`${this.base}/admin/accounts/${userId}/download`, {});
    }

    pause(userId: string): Observable<LolAdminAccount[]> {
        return this.http.post<LolAdminAccount[]>(`${this.base}/admin/accounts/${userId}/pause`, {});
    }

    downloadAll(): Observable<LolAdminAccount[]> {
        return this.http.post<LolAdminAccount[]>(`${this.base}/admin/accounts/download-all`, {});
    }
}
