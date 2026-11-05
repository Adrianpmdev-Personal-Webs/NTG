/**
 * CharactersModels
 *
 * Models of the characters screen. The class, the race and the professions are slugs of the game data; they and the
 * level come from the official armory and are null until it shows the character.
 *
 * @author @adrianpmdev
 */

export interface CharacterOwner {
    id: string;
    username: string | null;
}

export interface CharacterProfession {
    slug: string;
    skill: number | null;
    maxSkill: number | null;
}

export interface GameCharacter {
    id: string;
    owner: CharacterOwner;
    name: string;
    classSlug: string | null;
    raceSlug: string | null;
    level: number | null;
    professions: CharacterProfession[];
    notes: string | null;
    armoryUrl: string;
    canEdit: boolean;
}

export interface CharactersData {
    limit: number;
    owners: CharacterOwner[];
    characters: GameCharacter[];
}

export interface CharacterRequest {
    ownerId: string | null;
    name: string;
    notes: string | null;
}
