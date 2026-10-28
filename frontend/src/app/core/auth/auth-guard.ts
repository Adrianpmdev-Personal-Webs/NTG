/**
 * authGuard
 *
 * Route guard for protected application routes.
 * Allows access to authenticated users and restores the session from the refresh cookie when needed.
 *
 * @author @adrianpmdev
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthSession } from './auth-session';

export const authGuard: CanActivateFn = () => {
    const authSession = inject(AuthSession);
    const router = inject(Router);

    return authSession
        .ensureSession()
        .pipe(map((hasSession) => (hasSession ? true : router.createUrlTree(['/login']))));
};
