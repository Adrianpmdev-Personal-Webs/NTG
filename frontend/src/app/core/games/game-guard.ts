/**
 * gameGuard
 *
 * Builds a guard that allows a game's routes only when the backend catalog says the user can open the game: admins
 * always, players with the tag once the game is released. The backend applies the same rule to the game API.
 * It restores the session itself because guards of one route run in parallel (a page reload would otherwise
 * be evaluated before the user is known). Everyone else goes back to the root.
 *
 * @author @adrianpmdev
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of, switchMap } from 'rxjs';
import { AuthSession } from '../auth/auth-session';
import { GameCatalog } from './game-catalog';

export const gameGuard =
    (gameId: string): CanActivateFn =>
    () => {
        const authSession = inject(AuthSession);
        const catalog = inject(GameCatalog);
        const router = inject(Router);

        return authSession.ensureSession().pipe(
            switchMap((hasSession) => {
                if (!hasSession) {
                    return of(router.createUrlTree(['/login']));
                }

                return catalog
                    .canAccess(gameId)
                    .pipe(map((canAccess) => (canAccess ? true : router.createUrlTree(['/']))));
            }),
        );
    };
