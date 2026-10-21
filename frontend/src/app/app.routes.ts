/**
 * routes
 *
 * Application routes. Public auth pages, the terminal layout (welcome and admin) and one entry per game,
 * all lazy loaded. A new game adds its player routes and its admin routes here.
 *
 * @author @adrianpmdev
 */

import { Routes } from '@angular/router';
import { adminGuard } from '@core/guards/admin';
import { authGuard } from '@core/guards/auth';
import { gameGuard } from '@core/guards/game';
import { roleHomeGuard } from '@core/guards/role-home';

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
                    {
                        path: 'pokemon',
                        loadChildren: () => import('@features/pokemon/pokemon-admin.routes')
                            .then(m => m.POKEMON_ADMIN_ROUTES)
                    },
                    {
                        path: '',
                        loadChildren: () => import('@features/admin/admin.routes').then(m => m.ADMIN_ROUTES)
                    }
                ]
            }
        ]
    },
    {
        path: 'pokemon',
        canActivate: [authGuard, gameGuard('pokemon')],
        loadChildren: () => import('@features/pokemon/pokemon.routes').then(m => m.POKEMON_ROUTES)
    },
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
