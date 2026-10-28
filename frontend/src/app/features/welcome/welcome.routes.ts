/**
 * WELCOME_ROUTES
 *
 * Route of the welcome page shown to regular users after login.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const WELCOME_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () => import('./welcome').then((m) => m.Welcome),
    },
];
