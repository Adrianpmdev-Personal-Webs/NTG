/**
 * WOW_FOREVER_ROUTES
 *
 * Routes of the WoW Forever area for players. The launcher home is a full-screen page with its own look.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const WOW_FOREVER_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () => import('./home/home').then(m => m.Home)
    }
];
