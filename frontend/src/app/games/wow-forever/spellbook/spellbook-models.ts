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
    label: string;
    level: number | null;
    description: string;
    lines: TooltipLine[];
    school: string | null;
    coefficient: string | null;
    classic: ClassicStatus | null;
    classicDescription: string | null;
    learnedFrom: string | null;
    note: string | null;
}

export interface Spell {
    name: string;
    icon: string;
    fromTalent: boolean;
    ranks: SpellRank[];
}

export interface SpellbookTab {
    name: string;
    spells: Spell[];
}

export interface ClassSpellbook {
    gameClass: GameClass;
    tabs: SpellbookTab[];
    highlights: Ability[];
    removed: string[];
    credit: DataCredit;
}
