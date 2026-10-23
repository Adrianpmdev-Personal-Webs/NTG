/**
 * WowForeverConfig
 *
 * Static configuration of the WoW Forever area: icon source, faction labels and the list of features.
 *
 * @author @adrianpmdev
 */

import { Faction, LauncherNewsItem } from './wow-forever-models';

/** Where the game icons come from. Icons are stored by name, so changing the host is a one-line change. */
export const WOW_ICON_BASE_URL = 'https://render.worldofwarcraft.com/us/icons/56';

export const FACTION_LABELS: Readonly<Record<Faction, string>> = {
    ALLIANCE: 'Alianza',
    HORDE: 'Horda'
};

export interface WowFeature {
    id: string;
    label: string;
    description: string;
    route: string;
    /** False while the feature is not built yet: it is shown as coming soon and cannot be opened. */
    available: boolean;
}

/** Features of the area. Release one by setting `available: true` and adding its route. */
export const WOW_FEATURES: readonly WowFeature[] = [
    {
        id: 'talents',
        label: 'Calculadora de talentos',
        description: 'Planifica tu build de nivel 10 a 60.',
        route: '/wow-forever/talents',
        available: false
    },
    {
        id: 'dungeons',
        label: 'Dungeons y loot',
        description: 'Mazmorras, jefes y el botín de cada uno.',
        route: '/wow-forever/dungeons',
        available: false
    },
    {
        id: 'spellbook',
        label: 'Spellbook',
        description: 'Los hechizos de cada clase, rango a rango.',
        route: '/wow-forever/spellbook',
        available: false
    },
    {
        id: 'legacy',
        label: 'Legacy System',
        description: 'Perks y desafíos de la cuenta.',
        route: '/wow-forever/legacy',
        available: false
    },
    {
        id: 'races',
        label: 'Razas y racials',
        description: 'Habilidades raciales y combinaciones raza-clase.',
        route: '/wow-forever/races',
        available: false
    }
];

export const wowIconUrl = (icon: string): string => `${WOW_ICON_BASE_URL}/${icon}.jpg`;

/** Hides an icon image that could not be loaded; the name next to it still identifies the item. */
export const hideBrokenIcon = (event: Event): void => {
    (event.target as HTMLImageElement).style.visibility = 'hidden';
};

/** Faction artwork shown behind the launcher screen. */
export const FACTION_BACKGROUNDS: Readonly<Record<Faction, string>> = {
    ALLIANCE: '/images/wow-forever/background-alliance.webp',
    HORDE: '/images/wow-forever/background-horde.webp'
};

/** News shown in the launcher panel. Items without an image show the dark placeholder. */
export const LAUNCHER_NEWS: readonly LauncherNewsItem[] = [
    {
        id: 1,
        date: '4 de mayo de 2023',
        title: 'Embers of Neltharion: ¡Monturas, mascotas y mucho más!'
    },
    {
        id: 2,
        title: 'Adéntrate en las Sendas Temporales Turbulentas con Wrath of the Lich King'
    },
    {
        id: 3,
        title: 'Notas del parche, eventos y novedades de la comunidad'
    }
];
