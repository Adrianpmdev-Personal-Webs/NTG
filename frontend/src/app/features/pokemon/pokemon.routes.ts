/**
 * POKEMON_ROUTES
 *
 * Routes of the Pokemon game for players, rendered inside the Pokemon layout.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const POKEMON_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () => import('./pokemon-layout/pokemon-layout').then(m => m.PokemonLayout),
        children: [
            {
                path: '',
                pathMatch: 'full',
                redirectTo: 'lockes'
            },
            {
                path: 'lockes',
                loadComponent: () => import('./lockes/lockes').then(m => m.Lockes)
            },
            {
                path: 'lockes/:lockeId/tournaments',
                loadComponent: () => import('./tournaments/tournaments').then(m => m.Tournaments)
            },
            {
                path: 'type-calculator',
                loadComponent: () => import('./type-calculator/type-calculator').then(m => m.TypeCalculator)
            },
            {
                path: 'pokelike',
                loadComponent: () => import('./pokelike/pokelike-shell/pokelike-shell').then(m => m.PokelikeShell)
            }
        ]
    }
];
