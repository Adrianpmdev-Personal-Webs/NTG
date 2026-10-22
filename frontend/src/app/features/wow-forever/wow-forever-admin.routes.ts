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
        redirectTo: 'base-data'
    },
    {
        path: 'base-data',
        loadComponent: () => import('./admin/base-data/base-data').then(m => m.BaseData)
    }
];
