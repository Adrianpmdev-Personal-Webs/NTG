/**
 * routes
 *
 * Application routes. Public auth pages, the terminal layout (welcome and admin) and one entry per game,
 * all lazy loaded. The routes of the games are built from GAME_MODULES: each game is served at /<id> and its
 * management area at /admin/<id>, so adding a game does not touch this file.
 *
 * @author @adrianpmdev
 */

import { Route, Routes } from '@angular/router';
import { adminGuard } from '@core/guards/admin';
import { authGuard } from '@core/guards/auth';
import { gameGuard } from '@core/guards/game';
import { roleHomeGuard } from '@core/guards/role-home';
import { GAME_MODULES } from '@games/games';

const gameAdminRoutes: Route[] = GAME_MODULES
    .filter(game => game.adminRoutes)
    .map(game => ({ path: game.id, loadChildren: game.adminRoutes }));

const gamePlayerRoutes: Route[] = GAME_MODULES.map(game => ({
    path: game.id,
    canActivate: [authGuard, gameGuard(game.id)],
    loadChildren: game.routes
}));

export const routes: Routes = [
    {
        path: '',
        loadComponent: () => import('@layout/main-layout/main-layout').then(m => m.MainLayout),
        canActivate: [authGuard],
        children: [
            {
                path: '',
                pathMatch: 'full',
                canActivate: [roleHomeGuard],
                children: []
            },
            {
                path: 'welcome',
                loadChildren: () => import('@features/welcome/welcome.routes').then(m => m.WELCOME_ROUTES)
            },
            {
                path: 'admin',
                canActivate: [adminGuard],
                children: [
                    ...gameAdminRoutes,
                    {
                        path: '',
                        loadChildren: () => import('@features/admin/admin.routes').then(m => m.ADMIN_ROUTES)
                    }
                ]
            }
        ]
    },
    ...gamePlayerRoutes,
    {
        // Public routes. /set-password is the target of the link in the email sent by Supabase:
        // /set-password#access_token=XXX&...
        path: '',
        loadChildren: () => import('@features/auth/auth.routes').then(m => m.AUTH_ROUTES)
    },
    {
        path: '**',
        redirectTo: ''
    }
];
