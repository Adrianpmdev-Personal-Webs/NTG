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
        loadComponent: () => import('./home/home').then((m) => m.Home),
    },
    {
        path: 'talents',
        data: { visitLabel: 'Calculadora de talentos' },
        loadComponent: () => import('./talents/talent-calculator/talent-calculator').then((m) => m.TalentCalculator),
    },
    {
        path: 'spellbook',
        data: { visitLabel: 'Libro de hechizos' },
        loadComponent: () => import('./spellbook/spellbook-viewer/spellbook-viewer').then((m) => m.SpellbookViewer),
    },
    {
        path: 'legacy',
        data: { visitLabel: 'Sistema Legacy' },
        loadComponent: () => import('./legacy/legacy-planner/legacy-planner').then((m) => m.LegacyPlanner),
    },
    {
        path: 'races',
        data: { visitLabel: 'Razas' },
        loadComponent: () => import('./racials/racials-overview/racials-overview').then((m) => m.RacialsOverview),
    },
    {
        path: 'professions',
        data: { visitLabel: 'Professions' },
        loadComponent: () =>
            import('./professions/professions-overview/professions-overview').then((m) => m.ProfessionsOverview),
    },
    {
        path: 'characters',
        data: { visitLabel: 'Personajes' },
        loadComponent: () =>
            import('./characters/characters-directory/characters-directory').then((m) => m.CharactersDirectory),
    },
    {
        path: 'characters/:id',
        data: { visitLabel: 'Personaje' },
        loadComponent: () => import('./characters/character-sheet/character-sheet').then((m) => m.CharacterSheet),
    },
];
