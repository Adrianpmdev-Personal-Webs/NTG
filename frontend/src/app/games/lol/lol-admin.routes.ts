/**
 * LOL_ADMIN_ROUTES
 *
 * Management area of League of Legends.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const LOL_ADMIN_ROUTES: Routes = [
    { path: '', pathMatch: 'full', redirectTo: 'accounts' },
    {
        path: 'accounts',
        loadComponent: () => import('./admin/admin-accounts').then((m) => m.AdminAccounts),
    },
];
