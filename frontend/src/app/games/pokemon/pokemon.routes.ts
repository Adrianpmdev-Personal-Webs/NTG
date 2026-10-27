/**
 * POKEMON_ROUTES
 *
 * Player routes of the Pokémon module, inside its own layout: games, the randomlockes of a game, a
 * randomlocke, a tournament, the all-time ranking and the type calculator.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const POKEMON_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () => import('./pokemon-layout/pokemon-layout').then(m => m.PokemonLayout),
        children: [
            { path: '', pathMatch: 'full', redirectTo: 'games' },
            { path: 'games', loadComponent: () => import('./games/games').then(m => m.Games) },
            { path: 'games/:gameId', loadComponent: () => import('./game/game').then(m => m.GamePage) },
            { path: 'lockes/:lockeId', loadComponent: () => import('./locke/locke').then(m => m.LockePage) },
            { path: 'tournaments/:tournamentId', loadComponent: () => import('./tournament/tournament').then(m => m.TournamentPage) },
            { path: 'ranking', loadComponent: () => import('./ranking/ranking').then(m => m.RankingPage) },
            { path: 'type-calculator', loadComponent: () => import('./type-calculator/type-calculator').then(m => m.TypeCalculator) }
        ]
    }
];
