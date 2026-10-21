/**
 * AuthService
 *
 * Authentication service for the application.
 * Manages login, logout, in-memory access token storage and current user state.
 * The session survives page reloads through the HttpOnly refresh cookie: the guards and the interceptor
 * call refreshSession() when there is no access token or it has expired, and a timer renews it before it expires.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';
import { User } from '@shared/models/user';
import { API_BASE_URL } from '@core/config/api';
import { GAMES } from '@core/config/games';

export interface LoginRequest {
    email: string;
    password: string;
}

export interface AuthTokensResponse {
    accessToken: string;
    expiresIn: number;
    tokenType: string;
    user: User;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);

    private readonly baseUrl = `${API_BASE_URL}/auth`;

    /** The access token is renewed this many seconds before it expires. */
    private readonly refreshMarginSeconds = 60;
    private readonly minRefreshDelayMs = 5_000;
    private readonly millisecondsPerSecond = 1_000;

    private accessToken: string | null = null;
    private refreshInFlight$: Observable<AuthTokensResponse> | null = null;
    private refreshTimer: ReturnType<typeof setTimeout> | null = null;

    private readonly currentUserSubject = new BehaviorSubject<User | null>(null);

    currentUser$ = this.currentUserSubject.asObservable();

    login(request: LoginRequest): Observable<AuthTokensResponse> {
        return this.http.post<AuthTokensResponse>(`${this.baseUrl}/login`, request, {
            withCredentials: true
        }).pipe(
            tap(response => this.handleAuthResponse(response))
        );
    }

    /**
     * Restores or renews the session with the refresh cookie. Concurrent calls share one request, because
     * the refresh token is rotated on every use.
     */
    refreshSession(): Observable<AuthTokensResponse> {
        if (!this.refreshInFlight$) {
            this.refreshInFlight$ = this.http.post<AuthTokensResponse>(`${this.baseUrl}/refresh`, {}, {
                withCredentials: true
            }).pipe(
                tap(response => this.handleAuthResponse(response)),
                finalize(() => { this.refreshInFlight$ = null; }),
                shareReplay({ bufferSize: 1, refCount: false })
            );
        }

        return this.refreshInFlight$;
    }

    /** Emits true when there is a session, restoring it from the cookie if needed. Never errors. */
    ensureSession(): Observable<boolean> {
        if (this.isLoggedIn()) {
            return of(true);
        }

        return this.refreshSession().pipe(
            map(() => this.isLoggedIn()),
            catchError(() => of(false))
        );
    }

    /** Reloads the current user (role and tags) from the backend. */
    reloadUser(): void {
        this.http.get<User>(`${this.baseUrl}/me`).subscribe({
            next: user => this.currentUserSubject.next(user),
            error: () => { /* the interceptor handles an expired session */ }
        });
    }

    logout(): void {
        this.http.post<void>(`${this.baseUrl}/logout`, {}, {
            withCredentials: true
        }).subscribe({
            next: () => this.finishLogout(),
            error: () => this.finishLogout()
        });
    }

    /** Called when the session can no longer be renewed: clears it and goes to the login page. */
    expireSession(): void {
        this.finishLogout();
    }

    /**
     * Sets the password chosen from the email link and starts the session (the backend signs the user in).
     *
     * @param token access token found in the URL fragment of the email link
     */
    setPassword(token: string, password: string): Observable<AuthTokensResponse> {
        return this.http.put<AuthTokensResponse>(`${this.baseUrl}/set-password`, { token, password }, {
            withCredentials: true
        }).pipe(
            tap(response => this.handleAuthResponse(response))
        );
    }

    getToken(): string | null {
        return this.accessToken;
    }

    getCurrentUser(): User | null {
        return this.currentUserSubject.value;
    }

    isLoggedIn(): boolean {
        return !!this.accessToken;
    }

    isAdmin(): boolean {
        return this.getCurrentUserRole() === 'ADMIN';
    }

    isUser(): boolean {
        return this.getCurrentUserRole() === 'USUARIO';
    }

    /** Whether the user has the tag with this key. */
    hasTag(tagKey: string): boolean {
        return (this.getCurrentUser()?.tags ?? []).includes(tagKey);
    }

    /**
     * Admins can open every game, including upcoming ones (to develop them). The rest need the tag whose key is
     * the game id and the game must not be upcoming. The backend enforces the tag rule too.
     */
    canAccessGame(gameId: string): boolean {
        if (this.isAdmin()) {
            return true;
        }

        const game = GAMES.find(entry => entry.id === gameId);

        return !!game && !game.upcoming && this.hasTag(gameId);
    }

    private handleAuthResponse(response: AuthTokensResponse): void {
        this.accessToken = response.accessToken;
        this.currentUserSubject.next(response.user);
        this.scheduleRefresh(response.expiresIn);
    }

    private scheduleRefresh(expiresInSeconds: number): void {
        this.cancelScheduledRefresh();

        const delay = Math.max(
            (expiresInSeconds - this.refreshMarginSeconds) * this.millisecondsPerSecond,
            this.minRefreshDelayMs
        );

        this.refreshTimer = setTimeout(() => {
            this.refreshSession().subscribe({
                error: (error: unknown) => {
                    // Only a rejected session ends it; a network failure is retried by the interceptor later.
                    if (error instanceof HttpErrorResponse && error.status === 401) {
                        this.expireSession();
                    }
                }
            });
        }, delay);
    }

    private cancelScheduledRefresh(): void {
        if (this.refreshTimer !== null) {
            clearTimeout(this.refreshTimer);
            this.refreshTimer = null;
        }
    }

    private finishLogout(): void {
        this.clearSession();
        this.router.navigate(['/login']);
    }

    private clearSession(): void {
        this.cancelScheduledRefresh();
        this.accessToken = null;
        this.currentUserSubject.next(null);
    }

    private getCurrentUserRole(): string | null {
        return this.normalizeRole(this.getCurrentUser()?.role);
    }

    private normalizeRole(role?: string | null): string | null {
        if (!role) {
            return null;
        }

        return role.toUpperCase().replace('ROLE_', '');
    }
}
