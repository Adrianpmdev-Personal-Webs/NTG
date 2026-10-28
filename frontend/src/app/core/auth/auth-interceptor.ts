/**
 * authInterceptor
 *
 * HTTP interceptor that adds the Bearer access token to API requests. When the API answers 401 (the access
 * token expired) it renews the session with the refresh cookie once and repeats the request.
 * The /auth/ calls are never retried, so a failed refresh cannot loop.
 *
 * @author @adrianpmdev
 */

import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { API_BASE_URL } from '../api/api';
import { AuthSession } from './auth-session';

const UNAUTHORIZED = 401;

const withToken = (request: HttpRequest<unknown>, token: string): HttpRequest<unknown> =>
    request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

export const authInterceptor: HttpInterceptorFn = (request, next) => {
    const authSession = inject(AuthSession);
    const token = authSession.getToken();
    const authenticatedRequest = token ? withToken(request, token) : request;

    if (request.url.startsWith(`${API_BASE_URL}/auth/`)) {
        return next(authenticatedRequest);
    }

    return next(authenticatedRequest).pipe(
        catchError((error: unknown) => {
            if (!token || !(error instanceof HttpErrorResponse) || error.status !== UNAUTHORIZED) {
                return throwError(() => error);
            }

            return authSession.refreshSession().pipe(
                switchMap((response) => next(withToken(request, response.accessToken))),
                catchError((retryError: unknown) => {
                    if (retryError instanceof HttpErrorResponse && retryError.status === UNAUTHORIZED) {
                        authSession.expireSession();
                    }

                    return throwError(() => retryError);
                }),
            );
        }),
    );
};
