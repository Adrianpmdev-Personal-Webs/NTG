/**
 * ADMIN_ROUTES
 *
 * General admin routes: dashboard, users, the game catalog and the public pictures. The management of each game
 * lives in its own module.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
    },
    {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard').then((m) => m.Dashboard),
    },
    {
        path: 'users',
        loadComponent: () => import('./users/users').then((m) => m.Users),
    },
    {
        path: 'games',
        loadComponent: () => import('./games/games').then((m) => m.Games),
    },
    {
        path: 'images',
        loadComponent: () => import('./images/images').then((m) => m.Images),
    },
];
