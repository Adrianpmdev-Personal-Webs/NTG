/**
 * TalentsModels
 *
 * Models of a talent tree as the app works with it. The talent calculator and the Legacy planner share them: a Legacy
 * perk tree is a talent tree whose point gates count every point of the tree instead of the rows above.
 *
 * @author @adrianpmdev
 */

import { ClassicStatus, DataCredit, GameClass } from '../game-data/game-data-models';

/**
 * What a point gate counts: `rows-above` for talents (rows open every few points spent in the rows above),
 * `tree` for Legacy perks (points spent anywhere else in the tree).
 */
export type GateScope = 'rows-above' | 'tree';

export interface Talent {
    /** Unique in its class (or in the Legacy trees): the tree id and the position in the share code. */
    id: number;
    treeId: number;
    name: string;
    /** Zero based, top row first. */
    row: number;
    /** Zero based, left column first. */
    column: number;
    /** Points the talent takes. 0 means it cannot be bought (a slot kept for something still to come). */
    maxRanks: number;
    /** Points that must already be spent before it opens; see GateScope. */
    pointsRequired: number;
    /** Talents of the same tree; with several, any one of them at full rank is enough. */
    requires: number[];
    icon: string;
    /** Text of each rank; fewer texts than ranks repeat the last one, none when the talent has no text. */
    descriptions: string[];
    /** Cost line of an active talent, null for passives. */
    cost: string | null;
    /** How the talent compares with Classic, when the data says it. */
    classic: ClassicStatus | null;
}

export interface TalentTree {
    id: number;
    /** Lowercase name for file names, e.g. "beast-mastery". */
    slug: string;
    name: string;
    icon: string;
    rows: number;
    columns: number;
    gateScope: GateScope;
    /** In share code order (top row first, left to right): position N is digit N of the tree's part of a code. */
    talents: Talent[];
}

/** A talent under the pointer (or keyboard focus), with the screen position the tooltip card hangs from. */
export interface TalentHover {
    talent: Talent;
    x: number;
    y: number;
}

/** Everything the calculator needs for one class. */
export interface ClassTalents {
    gameClass: GameClass;
    trees: TalentTree[];
    credit: DataCredit;
}
