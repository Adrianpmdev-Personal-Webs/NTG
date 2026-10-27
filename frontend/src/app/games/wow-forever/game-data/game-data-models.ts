/**
 * GameDataModels
 *
 * Models shared by every WoW Forever tool: the classes, the credit line of the data and the abilities written as
 * name, text and icon.
 *
 * @author @adrianpmdev
 */

export interface GameClass {
    slug: string;
    name: string;
    icon: string;
}

/** What the licence of the data asks us to show, plus the game build it comes from when the file says so. */
export interface DataCredit {
    attribution: string;
    /** Day the data was generated. */
    generated: string;
    build: string | null;
}

export interface GameIndex {
    credit: DataCredit;
    classes: GameClass[];
}

export interface Ability {
    name: string;
    text: string;
    icon: string;
}

/** Comparison of a talent or spell with Classic Era. */
export type ClassicStatus = 'same' | 'moved' | 'changed' | 'new';
