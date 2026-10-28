/**
 * GameCatalogService
 *
 * Catalog of games from the backend: name, cover, whether it is coming soon and whether the current user can open it.
 * It is requested once per session and shared by the sidebar, the welcome page, the dashboard and the game guard;
 * it is requested again after an admin changes it, when tags may have changed (refresh) and after a new sign-in.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, distinctUntilChanged, map, of, shareReplay, tap, throwError } from 'rxjs';
import { API_BASE_URL } from '@core/config/api';
import { CatalogGame, CatalogGameRequest, CreateCatalogGameRequest } from '@shared/models/game';
import { AuthService } from './auth';

@Injectable({ providedIn: 'root' })
export class GameCatalogService {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/games`;

    private request$: Observable<CatalogGame[]> | null = null;

    /** Last catalog received (empty until the first answer). */
    readonly games = signal<CatalogGame[]>([]);

    constructor() {
        // A different user (or none) must never see the previous catalog.
        inject(AuthService).currentUser$.pipe(
            map(user => user?.id ?? null),
            distinctUntilChanged()
        ).subscribe(() => this.forget());
    }

    /** The catalog, requested only the first time. A failed request is forgotten so the next call tries again. */
    load(): Observable<CatalogGame[]> {
        if (!this.request$) {
            this.request$ = this.http.get<CatalogGame[]>(this.baseUrl).pipe(
                tap(games => this.games.set(games)),
                catchError(error => {
                    this.request$ = null;
                    return throwError(() => error);
                }),
                shareReplay({ bufferSize: 1, refCount: false })
            );
        }

        return this.request$;
    }

    /** Requests the catalog again (for example when the user's tags may have changed). Never errors. */
    refresh(): void {
        this.request$ = null;
        this.load().pipe(catchError(() => of([]))).subscribe();
    }

    /** Whether the current user can open a game. Never errors: a failed request means no access. */
    canAccess(gameId: string): Observable<boolean> {
        return this.load().pipe(
            map(games => games.some(game => game.id === gameId && game.accessible)),
            catchError(() => of(false))
        );
    }

    // ─── Admin ───

    create(request: CreateCatalogGameRequest): Observable<CatalogGame> {
        return this.http.post<CatalogGame>(this.baseUrl, request).pipe(tap(() => this.refresh()));
    }

    update(key: string, request: CatalogGameRequest): Observable<CatalogGame> {
        return this.http.put<CatalogGame>(`${this.baseUrl}/${key}`, request).pipe(tap(() => this.refresh()));
    }

    delete(key: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${key}`).pipe(tap(() => this.refresh()));
    }

    private forget(): void {
        this.request$ = null;
        this.games.set([]);
    }
}
