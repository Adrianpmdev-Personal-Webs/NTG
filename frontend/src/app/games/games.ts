/**
 * GAME_MODULES
 *
 * Game modules built into this web client. app.routes.ts creates their routes from this list, so adding a game is
 * creating its folder under games/ with its <game>.game.ts and listing it here. A game never imports another game.
 *
 * @author @adrianpmdev
 */

import { GameModule } from '@core/games/game-module';
import { POKEMON_GAME } from './pokemon/pokemon.game';
import { WOW_FOREVER_GAME } from './wow-forever/wow-forever.game';

export const GAME_MODULES: readonly GameModule[] = [POKEMON_GAME, WOW_FOREVER_GAME];
