/**
 * roleHomeGuard
 *
 * Redirects the root path to the landing page that matches the role: dashboard for admins, welcome for users.
 *
 * @author @adrianpmdev
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSession } from './auth-session';

export const roleHomeGuard: CanActivateFn = () => {
    const authSession = inject(AuthSession);
    const router = inject(Router);

    return router.createUrlTree([authSession.isAdmin() ? '/admin/dashboard' : '/welcome']);
};
