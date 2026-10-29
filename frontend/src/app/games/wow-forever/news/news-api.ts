/**
 * NewsApi
 *
 * HTTP service for the launcher news and blue posts of WoW Forever. Nothing is kept for the session: every visit to the
 * launcher asks again, and the browser gets a 304 with no body while they did not change.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { BluePostFeed, NewsFeed } from './news-models';

@Injectable({ providedIn: 'root' })
export class NewsApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/wow-forever`;

    getNews(): Observable<NewsFeed> {
        return this.http.get<NewsFeed>(`${this.baseUrl}/news`);
    }

    getBluePosts(): Observable<BluePostFeed> {
        return this.http.get<BluePostFeed>(`${this.baseUrl}/blue-posts`);
    }
}
