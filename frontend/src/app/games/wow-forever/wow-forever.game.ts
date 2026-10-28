/**
 * WOW_FOREVER_GAME
 *
 * Entry point of the WoW Forever module for the application shell: its id and its lazily loaded routes. It has no
 * management area: its data comes from the backend files and its pictures are managed in the general Images screen.
 *
 * @author @adrianpmdev
 */

import { GameModule } from '@core/games/game-module';

export const WOW_FOREVER_GAME: GameModule = {
    id: 'wow-forever',
    routes: () => import('./wow-forever.routes').then((m) => m.WOW_FOREVER_ROUTES),
};
