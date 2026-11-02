/**
 * LOL_GAME
 *
 * Entry point of the League of Legends module for the application shell: its id, its lazily loaded routes and its
 * management area, where an admin links accounts and starts the download of their games.
 *
 * @author @adrianpmdev
 */

import { GameModule } from '@core/games/game-module';

export const LOL_GAME: GameModule = {
    id: 'lol',
    routes: () => import('./lol.routes').then((m) => m.LOL_ROUTES),
    adminRoutes: () => import('./lol-admin.routes').then((m) => m.LOL_ADMIN_ROUTES),
    adminItems: [{ label: 'CUENTAS', route: '/admin/lol/accounts', icon: 'users' }],
};
