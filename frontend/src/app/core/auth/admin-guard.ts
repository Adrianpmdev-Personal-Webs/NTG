/**
 * adminGuard
 *
 * Allows access to admin routes only to users with the admin role; everyone else goes back to the root.
 *
 * @author @adrianpmdev
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSession } from './auth-session';

export const adminGuard: CanActivateFn = () => {
    const authSession = inject(AuthSession);
    const router = inject(Router);

    return authSession.isAdmin() ? true : router.createUrlTree(['/']);
};
