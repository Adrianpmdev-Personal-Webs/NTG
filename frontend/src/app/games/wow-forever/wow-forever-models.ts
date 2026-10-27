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
    date?: string;
    image?: string;
}

export interface WowSetting {
    key: string;
    value: string;
    description?: string;
    updatedAt: string;
}
