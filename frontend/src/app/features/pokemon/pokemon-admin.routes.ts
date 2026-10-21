/**
 * POKEMON_ADMIN_ROUTES
 *
 * Management routes of the Pokemon game (games, lockes and tournaments). They are mounted under
 * /admin/pokemon behind the admin guard and use the terminal layout.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const POKEMON_ADMIN_ROUTES: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'games'
    },
    {
        path: 'games',
        loadComponent: () => import('./admin/games/games').then(m => m.Games)
    },
    {
        path: 'games/:gameId/lockes',
        loadComponent: () => import('./admin/lockes/lockes').then(m => m.Lockes)
    },
    {
        path: 'games/:gameId/lockes/:lockeId/tournaments',
        loadComponent: () => import('./admin/tournament-detail/tournament-detail').then(m => m.TournamentDetail)
    }
];
