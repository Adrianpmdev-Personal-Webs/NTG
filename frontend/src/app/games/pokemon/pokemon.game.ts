/**
 * POKEMON_GAME
 *
 * Entry point of the Pokémon module for the application shell: its id, its lazily loaded routes and the links of
 * its management area.
 *
 * @author @adrianpmdev
 */

import { GameModule } from '@core/games/game-module';

export const POKEMON_GAME: GameModule = {
    id: 'pokemon',
    routes: () => import('./pokemon.routes').then((m) => m.POKEMON_ROUTES),
    adminRoutes: () => import('./pokemon-admin.routes').then((m) => m.POKEMON_ADMIN_ROUTES),
    adminItems: [
        { label: 'RANDOMLOCKES', route: '/admin/pokemon/lockes', icon: '▣' },
        { label: 'JUGADORES', route: '/admin/pokemon/players', icon: '◉' },
        { label: 'JUEGOS', route: '/admin/pokemon/games', icon: '◈' },
    ],
};
