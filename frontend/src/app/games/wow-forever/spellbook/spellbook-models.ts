/**
 * SpellbookModels
 *
 * Models of a class spellbook: tabs, spells and the tooltip of every rank.
 *
 * @author @adrianpmdev
 */

import { Ability, ClassicStatus, DataCredit, GameClass } from '../game-data/game-data-models';

export interface TooltipLine {
    left: string;
    right: string;
}

export interface SpellRank {
    /** Rank as the game writes it ("Rank 3"), empty for spells with a single rank. */
    label: string;
    /** Level the rank is learned at, when known. */
    level: number | null;
    description: string;
    lines: TooltipLine[];
    school: string | null;
    /** Spell power coefficient. */
    coefficient: string | null;
    classic: ClassicStatus | null;
    classicDescription: string | null;
    /** Where the rank is learned when it is not the class trainer. */
    learnedFrom: string | null;
    note: string | null;
}

export interface Spell {
    name: string;
    icon: string;
    /** A talent puts it in the book. */
    fromTalent: boolean;
    /** Lowest rank first. */
    ranks: SpellRank[];
}

export interface SpellbookTab {
    name: string;
    spells: Spell[];
}

export interface ClassSpellbook {
    gameClass: GameClass;
    tabs: SpellbookTab[];
    /** What changed for the class, in short. */
    highlights: Ability[];
    /** Classic spells the class no longer has. */
    removed: string[];
    credit: DataCredit;
}
