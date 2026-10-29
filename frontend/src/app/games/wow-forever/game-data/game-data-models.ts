/**
 * GameDataModels
 *
 * Models shared by every WoW Forever tool, as the backend sends them: the classes, the credit line of the data, the
 * abilities written as name, text and icon, and the index of the area (tools, factions, talent point levels).
 * Every `icon` is the full URL of the picture.
 *
 * @author @adrianpmdev
 */

import { Faction } from '../wow-forever-models';

export interface GameClass {
    slug: string;
    name: string;
    icon: string;
}

export interface DataCredit {
    attribution: string;
    generated: string;
    build: string | null;
}

export interface WowFeature {
    id: string;
    label: string;
    description: string;
    available: boolean;
}

export interface GameIndex {
    credit: DataCredit;
    classes: GameClass[];
    features: WowFeature[];
    factions: Record<Faction, string>;
    talentPointLevels: number[];
}

export interface Ability {
    name: string;
    text: string;
    icon: string;
    cooldown: string | null;
}

export type ClassicStatus = 'same' | 'moved' | 'changed' | 'new';
