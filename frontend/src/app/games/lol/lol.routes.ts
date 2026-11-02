/**
 * LOL_ROUTES
 *
 * Routes of the League of Legends area for players: a full-screen page with its own look.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const LOL_ROUTES: Routes = [
    {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./history/lol-history').then((m) => m.LolHistory),
    },
];
