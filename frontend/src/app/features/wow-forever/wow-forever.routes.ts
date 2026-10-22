/**
 * WOW_FOREVER_ROUTES
 *
 * Routes of the WoW Forever area for players, rendered inside its own layout.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const WOW_FOREVER_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () => import('./wow-forever-layout/wow-forever-layout').then(m => m.WowForeverLayout),
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./home/home').then(m => m.Home)
            }
        ]
    }
];
