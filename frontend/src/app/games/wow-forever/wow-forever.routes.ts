/**
 * WOW_FOREVER_ROUTES
 *
 * Routes of the WoW Forever area for players. The launcher home is a full-screen page with its own look; every tool is
 * loaded on demand.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const WOW_FOREVER_ROUTES: Routes = [
    {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./home/home').then(m => m.Home)
    },
    {
        path: 'talents',
        loadComponent: () => import('./talents/talent-calculator/talent-calculator').then(m => m.TalentCalculator)
    },
    {
        path: 'spellbook',
        loadComponent: () => import('./spellbook/spellbook-viewer/spellbook-viewer').then(m => m.SpellbookViewer)
    },
    {
        path: 'legacy',
        loadComponent: () => import('./legacy/legacy-planner/legacy-planner').then(m => m.LegacyPlanner)
    },
    {
        path: 'races',
        loadComponent: () => import('./racials/racials-overview/racials-overview').then(m => m.RacialsOverview)
    }
];
