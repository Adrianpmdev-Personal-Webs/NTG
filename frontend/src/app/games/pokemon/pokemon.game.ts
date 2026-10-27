/**
 * POKEMON_GAME
 *
 * Entry point of the Pokémon module for the application shell: its id and its lazily loaded routes.
 *
 * @author @adrianpmdev
 */

import { GameModule } from '@core/config/game-module';

export const POKEMON_GAME: GameModule = {
    id: 'pokemon',
    routes: () => import('./pokemon.routes').then(m => m.POKEMON_ROUTES),
    adminRoutes: () => import('./pokemon-admin.routes').then(m => m.POKEMON_ADMIN_ROUTES)
};
