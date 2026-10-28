/**
 * GameModule
 *
 * What a game module gives the application shell: its id, how to load its player and admin routes, and the links of
 * its management area. Everything else about a game (name, cover, coming soon, who can open it) comes from the
 * backend catalog, so this only holds what belongs to this web client.
 * The id is also the key of the tag that unlocks the game and its URL segment, so the shell serves the game at
 * /<id> (behind the auth and game guards) and its management area at /admin/<id> (behind the admin guard).
 * Adding a game never touches app.routes.ts: it exports one of these and is listed in games/games.ts.
 * It mirrors the GameModule each game declares in the backend.
 *
 * @author @adrianpmdev
 */

import { LoadChildrenCallback } from '@angular/router';

export interface GameAdminItem {
    label: string;
    route: string;
    icon: string;
}

export interface GameModule {
    id: string;
    routes: LoadChildrenCallback;
    adminRoutes?: LoadChildrenCallback;
    adminItems?: readonly GameAdminItem[];
}
