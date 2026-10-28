/**
 * TalentsModels
 *
 * Models of a talent tree as the app works with it. The talent calculator and the Legacy planner share them: a Legacy
 * perk tree is a talent tree whose point gates count every point of the tree instead of the rows above.
 *
 * @author @adrianpmdev
 */

import { ClassicStatus, DataCredit, GameClass } from '../game-data/game-data-models';

export type GateScope = 'rows-above' | 'tree';

export interface Talent {
    id: number;
    treeId: number;
    name: string;
    row: number;
    column: number;
    maxRanks: number;
    pointsRequired: number;
    requires: number[];
    icon: string;
    descriptions: string[];
    cost: string | null;
    classic: ClassicStatus | null;
}

export interface TalentTree {
    id: number;
    slug: string;
    name: string;
    icon: string;
    rows: number;
    columns: number;
    gateScope: GateScope;
    talents: Talent[];
}

export interface TalentHover {
    talent: Talent;
    x: number;
    y: number;
}

export interface ClassTalents {
    gameClass: GameClass;
    trees: TalentTree[];
    credit: DataCredit;
}
