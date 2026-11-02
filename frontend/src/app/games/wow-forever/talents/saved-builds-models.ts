/**
 * SavedBuildsModels
 *
 * A talent build a player keeps on their account: a fixed number of slots per class, each one empty or with a build.
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
