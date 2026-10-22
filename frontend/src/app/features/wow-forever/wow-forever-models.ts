/**
 * WowForeverModels
 *
 * Models and request bodies of the WoW Forever base data: classes, races and settings.
 *
 * @author @adrianpmdev
 */

export type Faction = 'ALLIANCE' | 'HORDE';

export interface WowClass {
    id: string;
    slug: string;
    name: string;
    icon: string;
    sortOrder: number;
    updatedAt: string;
}

export interface Race {
    id: string;
    slug: string;
    name: string;
    faction: Faction;
    icon: string;
    sortOrder: number;
    /** Ids of the classes this race can play. */
    classIds: string[];
    updatedAt: string;
}

export interface WowSetting {
    key: string;
    value: string;
    description?: string;
    updatedAt: string;
}

export interface WowClassCreateRequest {
    slug: string;
    name: string;
    icon: string;
    sortOrder: number;
}

export type WowClassUpdateRequest = Omit<WowClassCreateRequest, 'slug'>;

export interface RaceCreateRequest {
    slug: string;
    name: string;
    faction: Faction;
    icon: string;
    sortOrder: number;
    classIds: string[];
}

export type RaceUpdateRequest = Omit<RaceCreateRequest, 'slug'>;
