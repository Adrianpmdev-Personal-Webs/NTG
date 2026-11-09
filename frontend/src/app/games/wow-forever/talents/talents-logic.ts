/**
 * TalentsLogic
 *
 * Pure rules of a talent tree: unlocking, adding and removing points, points per level and the share code. The talent
 * calculator and the Legacy planner use the same rules; only what a point gate counts changes (see GateScope).
 * The talent calculator also keeps the order the points were spent in, one talent per point, written in the same short
 * code Wowhead uses: the tree index when the tree changes, then one letter per point, or a capital letter when every
 * rank of a talent was taken in a row. An order is only valid when each point was allowed at the moment it was spent.
 *
 * @author @adrianpmdev
 */

import { Talent, TalentTree } from './talents-models';

export type Ranks = Readonly<Record<number, number>>;

export type PointOrder = readonly number[];

export type TalentState = 'locked' | 'available' | 'partial' | 'maxed';

const DIGITS = '0123456789abcdefghijklmnopqrstuv';
const TREE_SEPARATOR = '-';
const TREE_ID_SEPARATOR = '.';
const ORDER_POINT = 'abcdefghjkmnpqrstvwzxyilou468-~!';
const ORDER_FULL_RANKS = 'ABCDEFGHJKMNPQRSTVWZXYILOU579_^.';
const ORDER_MAX_TREES = 4;

interface PlacedTalent {
    talent: Talent;
    tree: TalentTree;
    treeIndex: number;
    position: number;
}

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

export const sameRanks = (first: Ranks, second: Ranks): boolean => {
    const ids = new Set([...Object.keys(first), ...Object.keys(second)].map(Number));

    return [...ids].every((id) => (first[id] ?? 0) === (second[id] ?? 0));
};

export const ranksFromOrder = (order: PointOrder): Ranks => {
    const ranks: Record<number, number> = {};

    for (const id of order) {
        ranks[id] = (ranks[id] ?? 0) + 1;
    }

    return ranks;
};

const placeTalents = (trees: readonly TalentTree[]): Map<number, PlacedTalent> =>
    new Map(
        trees.flatMap((tree, treeIndex) =>
            tree.talents.map((talent, position): [number, PlacedTalent] => [
                talent.id,
                { talent, tree, treeIndex, position },
            ]),
        ),
    );

export const isOrderValid = (trees: readonly TalentTree[], order: PointOrder): boolean => {
    const placed = placeTalents(trees);
    const ranks: Record<number, number> = {};

    return order.every((id) => {
        const found = placed.get(id);

        if (
            !found ||
            rankOf(ranks, found.talent) >= found.talent.maxRanks ||
            !isUnlocked(found.talent, found.tree, ranks)
        ) {
            return false;
        }

        ranks[id] = rankOf(ranks, found.talent) + 1;
        return true;
    });
};

export const withoutLastPoint = (order: PointOrder, talent: Talent): PointOrder => {
    const last = order.lastIndexOf(talent.id);

    return last < 0 ? order : [...order.slice(0, last), ...order.slice(last + 1)];
};

export const canRemoveFromOrder = (talent: Talent, trees: readonly TalentTree[], order: PointOrder): boolean =>
    order.includes(talent.id) && isOrderValid(trees, withoutLastPoint(order, talent));

export const encodeOrder = (trees: readonly TalentTree[], order: PointOrder): string | null => {
    const placed = placeTalents(trees);
    let code = '';
    let treeIndex = -1;

    if (trees.length > ORDER_MAX_TREES) {
        return null;
    }

    for (const id of order) {
        const found = placed.get(id);

        if (!found || found.position >= ORDER_POINT.length) {
            return null;
        }

        if (found.treeIndex !== treeIndex) {
            treeIndex = found.treeIndex;
            code += treeIndex;
        }

        const point = ORDER_POINT.charAt(found.position);
        const fullRun = point.repeat(found.talent.maxRanks);

        code += point;

        if (found.talent.maxRanks > 1 && code.endsWith(fullRun)) {
            code = code.slice(0, -fullRun.length) + ORDER_FULL_RANKS.charAt(found.position);
        }
    }

    return code;
};

export const decodeOrder = (trees: readonly TalentTree[], code: string): PointOrder | null => {
    const order: number[] = [];
    let tree: TalentTree | undefined;

    if (trees.length > ORDER_MAX_TREES) {
        return null;
    }

    for (const char of code) {
        const treeIndex = Number.parseInt(char, 10);

        if (treeIndex < trees.length) {
            tree = trees[treeIndex];
            continue;
        }

        const point = ORDER_POINT.indexOf(char);
        const fullRanks = ORDER_FULL_RANKS.indexOf(char);
        const talent = tree?.talents[point >= 0 ? point : fullRanks];

        if (!talent) {
            return null;
        }

        for (let count = point >= 0 ? 1 : talent.maxRanks; count > 0; count--) {
            order.push(talent.id);
        }
    }

    return isOrderValid(trees, order) ? order : null;
};
