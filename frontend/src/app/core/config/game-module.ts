/**
 * GameModule
 *
 * What a game module gives the application shell: its id and how to load its player and admin routes.
 * The id is also the key of the tag that unlocks the game and its URL segment, so the shell serves the game at
 * /<id> (behind the auth and game guards) and its management area at /admin/<id> (behind the admin guard).
 * Adding a game never touches app.routes.ts: it exports one of these and is listed in games/games.ts.
 * It mirrors the GameModule each game declares in the backend.
 *
 * @author @adrianpmdev
 */

import { LoadChildrenCallback } from '@angular/router';

export interface GameModule {
    /** Game id, tag key and URL segment. */
    id: string;
    /** Player routes, loaded on demand. */
    routes: LoadChildrenCallback;
    /** Management routes for admins, loaded on demand. */
    adminRoutes?: LoadChildrenCallback;
}
