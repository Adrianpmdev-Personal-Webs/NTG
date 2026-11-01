/**
 * ProfileApi
 *
 * The profile of the signed-in user: their account and what each game they can open has about them. The texts of the
 * figures come ready from the backend, so this screen does not know any game.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';

export interface ProfileStat {
    label: string;
    value: string;
}

export interface ProfileEntry {
    title: string;
    detail: string;
}

export interface ProfileGame {
    id: string;
    name: string;
    iconUrl: string | null;
    stats: ProfileStat[];
    entries: ProfileEntry[];
}

export interface Profile {
    username: string;
    email: string;
    avatarUrl: string | null;
    role: string;
    createdAt: string;
    games: ProfileGame[];
}

@Injectable({ providedIn: 'root' })
export class ProfileApi {
    private readonly http = inject(HttpClient);

    getOwn(): Observable<Profile> {
        return this.http.get<Profile>(`${API_BASE_URL}/profile/me`);
    }
}
