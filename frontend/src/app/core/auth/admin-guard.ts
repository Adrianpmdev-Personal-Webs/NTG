/**
 * adminGuard
 *
 * Allows access to admin routes only to users with the admin role; everyone else goes back to the root.
 *
 * @author @adrianpmdev
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const adminGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.isAdmin() ? true : router.createUrlTree(['/']);
};
