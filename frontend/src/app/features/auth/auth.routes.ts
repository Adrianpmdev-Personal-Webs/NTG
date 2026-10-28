/**
 * AUTH_ROUTES
 *
 * Public routes of the auth feature: login and password setup.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
    {
        path: 'login',
        loadComponent: () => import('./login/login').then((m) => m.Login),
    },
    {
        path: 'set-password',
        loadComponent: () => import('./set-password/set-password').then((m) => m.SetPassword),
    },
];
