/**
 * WowForeverModels
 *
 * Models of the WoW Forever area that are not game data: factions, launcher news and the module settings.
 *
 * @author @adrianpmdev
 */

export type Faction = 'ALLIANCE' | 'HORDE';

export interface LauncherNewsItem {
    id: number;
    title: string;
    date?: string | null;
    image?: string | null;
}

export interface WowSetting {
    key: string;
    value: string;
    description?: string;
    updatedAt: string;
}
