/**
 * GAMES
 *
 * Central list of the games hosted by the site. It feeds the sidebar, the welcome page and the dashboard.
 * Adding a game means adding one entry here, its feature folder and its routes in app.routes.ts.
 * The id doubles as the tag key: users get access to a game when they have the tag with its id (admins always do).
 * The backend maps the same key to the game's API path, so keep the two in sync.
 * Set `upcoming: true` to lock a game: players see it as coming soon and cannot open it (the guard checks the flag),
 * while admins can still open it for development and see it marked with an asterisk.
 *
 * @author @adrianpmdev
 */

export interface GameAdminItem {
    label: string;
    route: string;
    icon: string;
}

export interface GameEntry {
    /** Game id and key of the tag that grants access to it. */
    id: string;
    name: string;
    imageUrl: string;
    /** Route of the game for players. */
    route: string;
    /** Route prefix of the game management area (admins only). */
    adminRoute?: string;
    adminItems: readonly GameAdminItem[];
    /** Coming soon: locked for players, open (and marked with *) for admins. */
    upcoming: boolean;
}

export const GAMES: readonly GameEntry[] = [
    {
        id: 'pokemon',
        name: 'Pokemon',
        imageUrl: '/images/games/pokemon.png',
        route: '/pokemon',
        adminRoute: '/admin/pokemon',
        adminItems: [
            { label: 'RANDOMLOCKES', route: '/admin/pokemon/lockes', icon: '▣' },
            { label: 'JUGADORES', route: '/admin/pokemon/players', icon: '◉' },
            { label: 'JUEGOS', route: '/admin/pokemon/games', icon: '◈' }
        ],
        upcoming: false
    },
    {
        id: 'wow-forever',
        name: 'WoW Forever',
        imageUrl: '/images/games/wow-forever.png',
        route: '/wow-forever',
        adminRoute: '/admin/wow-forever',
        adminItems: [
            { label: 'GESTIÓN', route: '/admin/wow-forever/management', icon: '▣' }
        ],
        upcoming: false
    },
    {
        id: 'lol',
        name: 'League of Legends',
        imageUrl: '/images/games/lol.png',
        route: '/lol',
        adminItems: [],
        upcoming: true
    },
    {
        id: 'cs2',
        name: 'Counter Strike 2',
        imageUrl: '/images/games/cs2.png',
        route: '/cs2',
        adminItems: [],
        upcoming: true
    }
];
