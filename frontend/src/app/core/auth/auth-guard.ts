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
import { AuthService } from '../services/auth';

export const authGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.ensureSession().pipe(
        map(hasSession => hasSession ? true : router.createUrlTree(['/login']))
    );
};
