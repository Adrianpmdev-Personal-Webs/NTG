/**
 * roleHomeGuard
 *
 * Redirects the root path to the landing page that matches the role: dashboard for admins, welcome for users.
 *
 * @author @adrianpmdev
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const roleHomeGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return router.createUrlTree([authService.isAdmin() ? '/admin/dashboard' : '/welcome']);
};
