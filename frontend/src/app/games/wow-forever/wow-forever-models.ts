/**
 * WowForeverModels
 *
 * Models of the WoW Forever area that are not game data: factions and launcher news.
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
