/**
 * AuthSession
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
import { BehaviorSubject, EMPTY, Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';
import { User } from '@shared/models/user';
import { API_BASE_URL } from '@core/api/api';

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
export class AuthSession {
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);

    private readonly baseUrl = `${API_BASE_URL}/auth`;

    private readonly refreshMarginSeconds = 60;
    private readonly minRefreshDelayMs = 5_000;
    private readonly millisecondsPerSecond = 1_000;

    private accessToken: string | null = null;
    private refreshInFlight$: Observable<AuthTokensResponse> | null = null;
    private refreshTimer: ReturnType<typeof setTimeout> | null = null;

    private readonly currentUserSubject = new BehaviorSubject<User | null>(null);

    currentUser$ = this.currentUserSubject.asObservable();

    login(request: LoginRequest): Observable<AuthTokensResponse> {
        return this.http
            .post<AuthTokensResponse>(`${this.baseUrl}/login`, request, {
                withCredentials: true,
            })
            .pipe(tap((response) => this.handleAuthResponse(response)));
    }

    refreshSession(): Observable<AuthTokensResponse> {
        if (!this.refreshInFlight$) {
            this.refreshInFlight$ = this.http
                .post<AuthTokensResponse>(
                    `${this.baseUrl}/refresh`,
                    {},
                    {
                        withCredentials: true,
                    },
                )
                .pipe(
                    tap((response) => this.handleAuthResponse(response)),
                    finalize(() => {
                        this.refreshInFlight$ = null;
                    }),
                    shareReplay({ bufferSize: 1, refCount: false }),
                );
        }

        return this.refreshInFlight$;
    }

    ensureSession(): Observable<boolean> {
        if (this.isLoggedIn()) {
            return of(true);
        }

        return this.refreshSession().pipe(
            map(() => this.isLoggedIn()),
            catchError(() => of(false)),
        );
    }

    reloadUser(): void {
        this.http
            .get<User>(`${this.baseUrl}/me`)
            .pipe(catchError(() => EMPTY))
            .subscribe((user) => this.currentUserSubject.next(user));
    }

    logout(): void {
        this.http
            .post<void>(
                `${this.baseUrl}/logout`,
                {},
                {
                    withCredentials: true,
                },
            )
            .subscribe({
                next: () => this.finishLogout(),
                error: () => this.finishLogout(),
            });
    }

    expireSession(): void {
        this.finishLogout();
    }

    setPassword(token: string, password: string): Observable<AuthTokensResponse> {
        return this.http
            .put<AuthTokensResponse>(
                `${this.baseUrl}/set-password`,
                { token, password },
                {
                    withCredentials: true,
                },
            )
            .pipe(tap((response) => this.handleAuthResponse(response)));
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

    private handleAuthResponse(response: AuthTokensResponse): void {
        this.accessToken = response.accessToken;
        this.currentUserSubject.next(response.user);
        this.scheduleRefresh(response.expiresIn);
    }

    private scheduleRefresh(expiresInSeconds: number): void {
        this.cancelScheduledRefresh();

        const delay = Math.max(
            (expiresInSeconds - this.refreshMarginSeconds) * this.millisecondsPerSecond,
            this.minRefreshDelayMs,
        );

        this.refreshTimer = setTimeout(() => {
            this.refreshSession().subscribe({
                error: (error: unknown) => {
                    if (error instanceof HttpErrorResponse && error.status === 401) {
                        this.expireSession();
                    }
                },
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
