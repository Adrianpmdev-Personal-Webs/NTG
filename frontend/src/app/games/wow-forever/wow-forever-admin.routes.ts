/**
 * WOW_FOREVER_ADMIN_ROUTES
 *
 * Routes of the WoW Forever management area (admins only), rendered inside the main layout.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const WOW_FOREVER_ADMIN_ROUTES: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'management'
    },
    {
        path: 'management',
        loadComponent: () => import('./admin/management/management').then(m => m.Management)
    }
];
