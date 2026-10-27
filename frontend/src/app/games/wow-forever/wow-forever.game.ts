/**
 * WOW_FOREVER_GAME
 *
 * Entry point of the WoW Forever module for the application shell: its id and its lazily loaded routes.
 *
 * @author @adrianpmdev
 */

import { GameModule } from '@core/config/game-module';

export const WOW_FOREVER_GAME: GameModule = {
    id: 'wow-forever',
    routes: () => import('./wow-forever.routes').then(m => m.WOW_FOREVER_ROUTES),
    adminRoutes: () => import('./wow-forever-admin.routes').then(m => m.WOW_FOREVER_ADMIN_ROUTES)
};
