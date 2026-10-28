/**
 * TalentsLogic
 *
 * Pure rules of a talent tree: unlocking, adding and removing points, points per level and the share code. The talent
 * calculator and the Legacy planner use the same rules; only what a point gate counts changes (see GateScope).
 *
 * @author @adrianpmdev
 */

import { Talent, TalentTree } from './talents-models';

export type Ranks = Readonly<Record<number, number>>;

export type TalentState = 'locked' | 'available' | 'partial' | 'maxed';

const DIGITS = '0123456789abcdefghijklmnopqrstuv';
const TREE_SEPARATOR = '-';
const TREE_ID_SEPARATOR = '.';

const rankOf = (ranks: Ranks, talent: Talent): number => ranks[talent.id] ?? 0;

export const pointsInTree = (tree: TalentTree, ranks: Ranks): number =>
    tree.talents.reduce((sum, talent) => sum + rankOf(ranks, talent), 0);

export const pointsAboveRow = (tree: TalentTree, ranks: Ranks, row: number): number =>
    tree.talents.reduce((sum, talent) => (talent.row < row ? sum + rankOf(ranks, talent) : sum), 0);

export const pointsTowardsGate = (tree: TalentTree, ranks: Ranks, talent: Talent): number =>
    tree.gateScope === 'tree'
        ? pointsInTree(tree, ranks) - rankOf(ranks, talent)
        : pointsAboveRow(tree, ranks, talent.row);

export const totalPoints = (trees: readonly TalentTree[], ranks: Ranks): number =>
    trees.reduce((sum, tree) => sum + pointsInTree(tree, ranks), 0);

export const prerequisitesOf = (tree: TalentTree, talent: Talent): Talent[] =>
    tree.talents.filter((other) => talent.requires.includes(other.id));

export const isUnlocked = (talent: Talent, tree: TalentTree, ranks: Ranks): boolean => {
    if (pointsTowardsGate(tree, ranks, talent) < talent.pointsRequired) {
        return false;
    }

    const prerequisites = prerequisitesOf(tree, talent);

    return prerequisites.length === 0 || prerequisites.some((required) => rankOf(ranks, required) >= required.maxRanks);
};

export const canAdd = (talent: Talent, tree: TalentTree, ranks: Ranks, pointsLeft: number): boolean =>
    pointsLeft > 0 && rankOf(ranks, talent) < talent.maxRanks && isUnlocked(talent, tree, ranks);

export const isValid = (tree: TalentTree, ranks: Ranks): boolean =>
    tree.talents.every((talent) => {
        const rank = rankOf(ranks, talent);

        return rank === 0 || (rank <= talent.maxRanks && isUnlocked(talent, tree, ranks));
    });

export const canRemove = (talent: Talent, tree: TalentTree, ranks: Ranks): boolean => {
    const rank = rankOf(ranks, talent);

    return rank > 0 && isValid(tree, { ...ranks, [talent.id]: rank - 1 });
};

export const talentState = (talent: Talent, tree: TalentTree, ranks: Ranks, pointsLeft: number): TalentState => {
    const rank = rankOf(ranks, talent);

    if (talent.maxRanks === 0) {
        return 'locked';
    }

    if (rank >= talent.maxRanks) {
        return 'maxed';
    }

    if (rank > 0) {
        return 'partial';
    }

    return canAdd(talent, tree, ranks, pointsLeft) ? 'available' : 'locked';
};

export const rankText = (talent: Talent, rank: number): string | null =>
    talent.descriptions.length === 0 ? null : talent.descriptions[Math.min(rank, talent.descriptions.length) - 1];

export const pointsAtLevel = (levels: readonly number[], level: number): number =>
    levels.filter((granting) => granting <= level).length;

export const minLevelFor = (levels: readonly number[], spent: number): number =>
    spent <= 0 ? levels[0] : levels[Math.min(spent, levels.length) - 1];

export const encodeBuild = (trees: readonly TalentTree[], ranks: Ranks): string =>
    trees
        .map((tree) => {
            const digits = tree.talents
                .map((talent) => DIGITS.charAt(rankOf(ranks, talent)))
                .join('')
                .replace(/0+$/, '');

            return digits ? `${tree.id}${TREE_ID_SEPARATOR}${digits}` : '';
        })
        .filter((group) => group !== '')
        .join(TREE_SEPARATOR);

export const decodeBuild = (trees: readonly TalentTree[], code: string): Ranks => {
    const ranks: Record<number, number> = {};

    for (const group of code.split(TREE_SEPARATOR)) {
        const dot = group.indexOf(TREE_ID_SEPARATOR);
        const tree = dot > 0 ? trees.find((candidate) => candidate.id === Number(group.slice(0, dot))) : undefined;
        const digits = group.slice(dot + 1);

        if (!tree || digits.length > tree.talents.length || !/^[0-9a-v]*$/.test(digits)) {
            return {};
        }

        tree.talents.forEach((talent, position) => {
            const rank = position < digits.length ? DIGITS.indexOf(digits.charAt(position)) : 0;

            if (rank > 0) {
                ranks[talent.id] = rank;
            }
        });

        if (!isValid(tree, ranks)) {
            return {};
        }
    }

    return ranks;
};
