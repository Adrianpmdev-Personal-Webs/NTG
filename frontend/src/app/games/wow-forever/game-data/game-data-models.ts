/**
 * GameDataModels
 *
 * Models shared by every WoW Forever tool, as the backend sends them: the classes, the credit line of the data, the
 * abilities written as name, text and icon, and the index of the area (tools, news, factions, talent point levels).
 * Every `icon` is the full URL of the picture.
 *
 * @author @adrianpmdev
 */

import { Faction, LauncherNewsItem } from '../wow-forever-models';

export interface GameClass {
    slug: string;
    name: string;
    /** Icon URL. */
    icon: string;
}

/** What the licence of the data asks us to show, plus the game build it comes from when the file says so. */
export interface DataCredit {
    attribution: string;
    /** Day the data was generated. */
    generated: string;
    build: string | null;
}

/** A tool of the area. Its id is also its route segment (/wow-forever/<id>). */
export interface WowFeature {
    id: string;
    label: string;
    description: string;
    /** False while the tool is not built yet: it is shown as coming soon and cannot be opened. */
    available: boolean;
}

export interface GameIndex {
    credit: DataCredit;
    classes: GameClass[];
    features: WowFeature[];
    news: LauncherNewsItem[];
    /** Name of each faction. */
    factions: Record<Faction, string>;
    /** Levels that give a talent point, lowest first. */
    talentPointLevels: number[];
}

export interface Ability {
    name: string;
    text: string;
    /** Icon URL. */
    icon: string;
}

/** Comparison of a talent or spell with Classic Era. */
export type ClassicStatus = 'same' | 'moved' | 'changed' | 'new';
