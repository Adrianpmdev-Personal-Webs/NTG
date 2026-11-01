/**
 * SavedBuildsStorage
 *
 * The talent builds a player keeps in this browser: a fixed number of slots per class. What is stored is checked when
 * it is read, so a damaged or hand-edited value only leaves empty slots.
 *
 * @author @adrianpmdev
 */

export interface SavedBuild {
    name: string;
    level: number;
    build: string;
}

export type SavedBuildSlots = readonly (SavedBuild | null)[];

export const SLOTS_PER_CLASS = 5;
export const SAVED_BUILD_NAME_MAX_LENGTH = 30;

const storageKey = (classSlug: string): string => `ntg.wow-forever.talents.builds.${classSlug}`;

const isSavedBuild = (value: unknown): value is SavedBuild => {
    const candidate = value as Partial<SavedBuild> | null;

    return (
        typeof candidate?.name === 'string' &&
        typeof candidate.level === 'number' &&
        Number.isFinite(candidate.level) &&
        typeof candidate.build === 'string'
    );
};

export const readSavedBuilds = (classSlug: string): SavedBuildSlots => {
    let stored: unknown[] = [];

    try {
        const parsed: unknown = JSON.parse(localStorage.getItem(storageKey(classSlug)) ?? '[]');

        stored = Array.isArray(parsed) ? parsed : [];
    } catch {
        stored = [];
    }

    return Array.from({ length: SLOTS_PER_CLASS }, (_, index) => {
        const value = stored[index];

        return isSavedBuild(value) ? value : null;
    });
};

export const writeSavedBuilds = (classSlug: string, slots: SavedBuildSlots): boolean => {
    try {
        localStorage.setItem(storageKey(classSlug), JSON.stringify(slots));
        return true;
    } catch {
        return false;
    }
};
