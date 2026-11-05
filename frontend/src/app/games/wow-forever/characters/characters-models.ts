/**
 * CharactersModels
 *
 * Models of the characters screen. The class, the race and the professions are slugs of the game data.
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
}

export interface GameCharacter {
    id: string;
    owner: CharacterOwner;
    name: string;
    classSlug: string;
    raceSlug: string;
    level: number;
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
    classSlug: string;
    raceSlug: string;
    level: number;
    professions: CharacterProfession[];
    notes: string | null;
}
