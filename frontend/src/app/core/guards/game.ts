/**
 * gameGuard
 *
 * Builds a guard that allows a game's routes only to admins and to users that have the tag with the game id.
 * It restores the session itself because guards of one route run in parallel (a page reload would otherwise
 * be evaluated before the user is known). Everyone else goes back to the root.
 *
 * @author @adrianpmdev
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../services/auth';

export const gameGuard = (gameId: string): CanActivateFn => () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.ensureSession().pipe(
        map(hasSession => {
            if (!hasSession) {
                return router.createUrlTree(['/login']);
            }

            return authService.canAccessGame(gameId) ? true : router.createUrlTree(['/']);
        })
    );
};
