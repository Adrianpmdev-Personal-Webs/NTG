/**
 * ADMIN_ROUTES
 *
 * General admin routes: dashboard and user management. Game management lives in each game feature.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
    },
    {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard').then(m => m.Dashboard)
    },
    {
        path: 'users',
        loadComponent: () => import('./users/users').then(m => m.Users)
    }
];
