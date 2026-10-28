/**
 * POKEMON_ADMIN_ROUTES
 *
 * Management screens of the Pokémon module, inside the terminal shell: games, players, randomlockes,
 * one randomlocke (roster and tournaments) and one tournament.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';

export const POKEMON_ADMIN_ROUTES: Routes = [
    { path: '', pathMatch: 'full', redirectTo: 'lockes' },
    { path: 'games', loadComponent: () => import('./admin/games/admin-games').then((m) => m.AdminGames) },
    { path: 'players', loadComponent: () => import('./admin/players/admin-players').then((m) => m.AdminPlayers) },
    { path: 'lockes', loadComponent: () => import('./admin/lockes/admin-lockes').then((m) => m.AdminLockes) },
    { path: 'lockes/:lockeId', loadComponent: () => import('./admin/locke/admin-locke').then((m) => m.AdminLocke) },
    {
        path: 'tournaments/:tournamentId',
        loadComponent: () => import('./admin/tournament/admin-tournament').then((m) => m.AdminTournament),
    },
];
