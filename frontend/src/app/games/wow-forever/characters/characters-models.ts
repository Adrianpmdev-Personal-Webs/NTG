/**
 * CharactersModels
 *
 * Models of the characters screens. Only the name and the owner are stored; the rest comes from the official armory
 * through the backend, in English.
 *
 * @author @adrianpmdev
 */

import { Faction } from '../wow-forever-models';

export interface CharacterOwner {
    id: string;
    username: string | null;
}

export interface GameCharacter {
    id: string;
    owner: CharacterOwner;
    name: string;
    armoryUrl: string;
    canEdit: boolean;
}

export interface CharactersData {
    myUserId: string | null;
    limit: number;
    owners: CharacterOwner[];
    characters: GameCharacter[];
}

export interface CharacterRequest {
    ownerId: string | null;
    name: string;
}

export type ArmoryStatus = 'FOUND' | 'NOT_FOUND' | 'FAILED';

export interface ArmoryProfession {
    name: string;
    slug: string;
    skill: number;
    maxSkill: number;
    icon: string | null;
}

export interface CharacterSummary {
    id: string;
    status: ArmoryStatus;
    level: number | null;
    className: string | null;
    classSlug: string | null;
    raceName: string | null;
    raceSlug: string | null;
    professions: ArmoryProfession[];
}

export interface TooltipLine {
    left: string;
    right: string | null;
    color: string | null;
}

export interface ArmoryItem {
    slot: string;
    slotName: string;
    name: string;
    quality: string;
    icon: string | null;
    lines: TooltipLine[];
}

export interface ArmoryStat {
    name: string;
    value: string;
}

export interface ArmoryStatGroup {
    name: string;
    stats: ArmoryStat[];
}

export interface ArmoryTalent {
    name: string;
    rank: number;
    icon: string | null;
    cast: string | null;
    description: string;
}

export interface ArmorySpec {
    name: string;
    icon: string | null;
    points: number;
    talents: ArmoryTalent[];
}

export interface ArmorySheet {
    name: string;
    level: number;
    className: string;
    classSlug: string;
    raceName: string;
    raceSlug: string;
    faction: Faction | null;
    gender: string | null;
    guild: string | null;
    averageItemLevel: number;
    lastUpdated: string | null;
    images: { avatar: string | null; render: string | null; background: string | null };
    professions: ArmoryProfession[];
    gear: ArmoryItem[];
    stats: ArmoryStatGroup[];
    specs: ArmorySpec[];
    honorableKills: number;
}

export interface CharacterSheet {
    id: string;
    name: string;
    owner: CharacterOwner;
    armoryUrl: string;
    status: ArmoryStatus;
    sheet: ArmorySheet | null;
}
