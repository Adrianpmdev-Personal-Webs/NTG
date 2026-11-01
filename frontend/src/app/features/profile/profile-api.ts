/**
 * ProfileApi
 *
 * The profile of the signed-in user: their account and what each game they can open has about them. A game answers with
 * sections made of common pieces (figures, bars and cards), so this screen draws any game without knowing it.
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

export interface ProfileBar {
    label: string;
    current: number | null;
    max: number;
    iconUrl: string | null;
}

export interface ProfileCard {
    title: string;
    subtitle: string;
    iconUrl: string | null;
    bars: ProfileBar[];
}

export interface ProfileSection {
    title: string;
    stats: ProfileStat[];
    bars: ProfileBar[];
    cards: ProfileCard[];
}

export interface ProfileGame {
    id: string;
    name: string;
    iconUrl: string | null;
    coverUrl: string | null;
    summary: string[];
    sections: ProfileSection[];
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
