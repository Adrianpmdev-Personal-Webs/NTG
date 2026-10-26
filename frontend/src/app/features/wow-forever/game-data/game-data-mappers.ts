/**
 * GameDataMappers
 *
 * Turns the responses of the game data API (sections of data.json as they are in the file) into the models of each
 * tool. This is the only place that knows the format of the file: if it changes, only this file changes. Every field a
 * hand-edited file might leave out has a safe default, so a missing value shows as empty instead of breaking a screen.
 *
 * @author @adrianpmdev
 */

import { ChallengeGroup, LegacyData } from '../legacy/legacy-models';
import { PLACEHOLDER_PERK_NAME } from '../legacy/legacy-config';
import { ClassRacials, Race, RacialsData } from '../racials/racials-models';
import { GENERAL_TAB_NAME, LEARNED_FROM_LABELS, LEARNED_FROM_QUEST } from '../spellbook/spellbook-config';
import { ClassSpellbook, Spell, SpellRank, SpellbookTab } from '../spellbook/spellbook-models';
import { POINTS_PER_TALENT_ROW, TALENT_TREE_IDS } from '../talents/talents-config';
import { ClassTalents, GateScope, Talent, TalentTree } from '../talents/talents-models';
import { Faction } from '../wow-forever-models';
import {
    AbilityFile,
    ClassicStatusFile,
    DataClassFile,
    DataIndexFile,
    DataMetaFile,
    LegacyResponseFile,
    RacialsResponseFile,
    SpellRefFile,
    SpellTextFile,
    SpellbookResponseFile,
    TalentsResponseFile
} from './game-data-files';
import { Ability, ClassicStatus, DataCredit, GameClass, GameIndex } from './game-data-models';

/** Icon for anything the file gives no icon for: the game's question mark. */
export const FALLBACK_ICON = 'inv_misc_questionmark';

/** Talent ids are `<tree id> * 100 + <position>`, so they are unique in a class as long as a tree has under 100. */
const TALENT_ID_FACTOR = 100;

const BUILD_PATTERN = /\d+\.\d+\.\d+\.\d+/;
const RANK_PATTERN = /(\d+)/;
const LEVEL_PATTERN = /level (\d+)/i;

/* ─── Shared ─── */

/** Lower case, words joined by dashes: "Night Elf" becomes "night-elf". The backend builds class slugs the same way. */
export const slugify = (text: string): string =>
    text
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');

/** The first game build (1.60.1.70425) found in the texts, or null. */
const buildOf = (...texts: (string | undefined)[]): string | null => {
    for (const text of texts) {
        const match = text ? BUILD_PATTERN.exec(text) : null;

        if (match) {
            return match[0];
        }
    }

    return null;
};

const toCredit = (meta: DataMetaFile | undefined, ...sources: (string | undefined)[]): DataCredit => ({
    attribution: meta?.attribution ?? '',
    generated: meta?.generated ?? '',
    build: buildOf(...sources)
});

const toClassicStatus = (status: ClassicStatusFile | undefined): ClassicStatus | null =>
    status === 'same' || status === 'moved' || status === 'changed' || status === 'new' ? status : null;

const toGameClass = (file: DataClassFile): GameClass => ({
    slug: file.slug,
    name: file.name,
    icon: file.icon || FALLBACK_ICON
});

const toAbility = ([name, text, icon]: AbilityFile): Ability => ({
    name: name ?? '',
    text: text ?? '',
    icon: icon || FALLBACK_ICON
});

export const toGameIndex = (file: DataIndexFile): GameIndex => ({
    credit: toCredit(file.meta),
    classes: (file.classes ?? []).map(toGameClass)
});

/* ─── Trees (talents and Legacy perks) ─── */

interface TreeItem {
    name: string;
    maxRanks: number;
    /** One based, as in the file. */
    row: number;
    /** One based, as in the file. */
    column: number;
    icon: string;
    descriptions: string[];
    /** Name of the item it needs, if any. */
    requires: string | undefined;
    pointsRequired: number;
    cost: string | null;
    classic: ClassicStatus | null;
}

interface TreeSource {
    id: number;
    name: string;
    icon: string;
    gateScope: GateScope;
    items: TreeItem[];
}

/**
 * Builds a tree: items in share code order (top row first, left to right), prerequisites resolved by name inside the
 * tree. A prerequisite that does not exist is dropped, and so is one of two items that need each other, so a
 * hand-edited file can never lock a tree.
 */
const toTree = (source: TreeSource): TalentTree => {
    const items = [...source.items].sort((a, b) => a.row - b.row || a.column - b.column);
    const idOf = (position: number): number => source.id * TALENT_ID_FACTOR + position;
    const positionByName = new Map<string, number>();

    items.forEach((item, position) => {
        if (!positionByName.has(item.name)) {
            positionByName.set(item.name, position);
        }
    });

    const requiredPosition = (item: TreeItem, position: number): number | undefined => {
        const required = item.requires === undefined ? undefined : positionByName.get(item.requires);

        return required === undefined || required === position ? undefined : required;
    };

    const talents: Talent[] = items.map((item, position) => {
        const required = requiredPosition(item, position);
        // Two items that need each other: only the one further down (or to the right) keeps its prerequisite.
        const mutual = required !== undefined && requiredPosition(items[required], required) === position;

        return {
            id: idOf(position),
            treeId: source.id,
            name: item.name,
            row: Math.max(0, item.row - 1),
            column: Math.max(0, item.column - 1),
            maxRanks: Math.max(0, item.maxRanks),
            pointsRequired: Math.max(0, item.pointsRequired),
            requires: required === undefined || (mutual && required > position) ? [] : [idOf(required)],
            icon: item.icon || FALLBACK_ICON,
            descriptions: item.descriptions,
            cost: item.cost,
            classic: item.classic
        };
    });

    return {
        id: source.id,
        slug: slugify(source.name),
        name: source.name,
        icon: source.icon || FALLBACK_ICON,
        rows: Math.max(1, ...talents.map(talent => talent.row + 1)),
        columns: Math.max(1, ...talents.map(talent => talent.column + 1)),
        gateScope: source.gateScope,
        talents
    };
};

/* ─── Talents ─── */

export const toClassTalents = (file: TalentsResponseFile): ClassTalents => {
    const gameClass = toGameClass(file.class);
    const trees = (file.talents.trees ?? []).map((tree, index) =>
        toTree({
            id: TALENT_TREE_IDS[`${gameClass.slug}-${slugify(tree.name)}`] ?? index + 1,
            name: tree.name,
            icon: tree.icon,
            gateScope: 'rows-above',
            items: (tree.talents ?? []).map(talent => ({
                name: talent.name,
                maxRanks: talent.max,
                row: talent.row,
                column: talent.col,
                icon: talent.icon,
                descriptions: talent.desc ?? [],
                requires: talent.req,
                pointsRequired: (talent.row - 1) * POINTS_PER_TALENT_ROW,
                cost: talent.cost ?? null,
                classic: toClassicStatus(talent.classic?.status)
            }))
        })
    );

    return { gameClass, trees, credit: toCredit(file.meta, file.talents.source) };
};

/* ─── Legacy ─── */

export const toLegacy = (file: LegacyResponseFile): LegacyData => {
    const legacy = file.legacy;
    const trees = (legacy.trees ?? []).map(tree =>
        toTree({
            id: tree.id,
            name: tree.name,
            icon: tree.icon,
            gateScope: 'tree',
            items: (tree.perks ?? []).map(perk => ({
                name: perk.placeholder ? PLACEHOLDER_PERK_NAME : perk.name,
                maxRanks: perk.placeholder ? 0 : perk.max,
                row: perk.row,
                column: perk.col,
                icon: perk.icon,
                descriptions: perk.ranks ?? [],
                requires: perk.placeholder ? undefined : perk.req,
                pointsRequired: perk.gate ?? 0,
                cost: perk.cost ?? null,
                classic: null
            }))
        })
    );

    const groups = new Map<string, ChallengeGroup>();

    for (const challenge of legacy.challenges ?? []) {
        const group = groups.get(challenge.group) ?? { name: challenge.group, challenges: [], points: 0 };
        const points = challenge.points ?? 1;

        group.challenges.push({ name: challenge.name, description: challenge.desc ?? '', points });
        group.points += points;
        groups.set(challenge.group, group);
    }

    const challengeGroups = [...groups.values()];

    return {
        points: legacy.points ?? 0,
        trees,
        challengeGroups,
        challengePoints: challengeGroups.reduce((sum, group) => sum + group.points, 0),
        credit: toCredit(file.meta, legacy.source)
    };
};

/* ─── Spellbook ─── */

const learnedFrom = (text: SpellTextFile | undefined): string | null => {
    if (text?.ns === 'quest') {
        return LEARNED_FROM_QUEST;
    }

    return text?.nt === undefined ? null : LEARNED_FROM_LABELS[text.nt] ?? null;
};

const levelOf = (text: SpellTextFile | undefined, fallback: number | undefined): number | null => {
    const match = text?.lv ? LEVEL_PATTERN.exec(text.lv) : null;

    return match ? Number(match[1]) : fallback ?? null;
};

const toRank = (label: string, text: SpellTextFile | undefined, fallbackLevel: number | undefined): SpellRank => ({
    label,
    level: levelOf(text, fallbackLevel),
    description: text?.d ?? '',
    lines: (text?.l ?? [])
        .map(([left, right]) => ({ left: left ?? '', right: right ?? '' }))
        .filter(line => line.left !== '' || line.right !== ''),
    school: text?.sc || null,
    coefficient: text?.co || null,
    classic: toClassicStatus(text?.cs),
    classicDescription: text?.cd || null,
    learnedFrom: learnedFrom(text),
    note: text?.fx || null
});

const rankNumber = (label: string): number => Number(RANK_PATTERN.exec(label)?.[1] ?? 0);

export const toClassSpellbook = (file: SpellbookResponseFile): ClassSpellbook => {
    const book = file.spellbook;
    const texts = file.spells ?? {};
    const levels = book.levels ?? {};
    const icons = book.icons ?? {};
    const fromTalent = new Set(book.talents ?? []);

    /** One spell per name, its ranks in order, in the order the book first lists each name. */
    const toSpells = (refs: SpellRefFile[]): Spell[] => {
        const ranksByName = new Map<string, string[]>();

        for (const [name, rank] of refs) {
            ranksByName.set(name, [...(ranksByName.get(name) ?? []), rank ?? '']);
        }

        return [...ranksByName].map(([name, labels]) => {
            const sorted = [...labels].sort((a, b) => rankNumber(a) - rankNumber(b));

            return {
                name,
                icon: icons[name] || FALLBACK_ICON,
                fromTalent: fromTalent.has(name),
                ranks: sorted.map((label, index) => toRank(label, texts[`${name}|${label}`], levels[name]?.[index]))
            };
        });
    };

    const tabs: SpellbookTab[] = [
        { name: GENERAL_TAB_NAME, spells: toSpells(book.general ?? []) },
        ...(book.tabs ?? []).map(tab => ({ name: tab.name, spells: toSpells(tab.spells ?? []) }))
    ].filter(tab => tab.spells.length > 0);

    return {
        gameClass: toGameClass(file.class),
        tabs,
        highlights: (file.highlights ?? []).map(toAbility),
        removed: book.gone ?? [],
        credit: toCredit(file.meta, book.build)
    };
};

/* ─── Racials ─── */

const toFaction = (key: string): Faction => (key.toLowerCase() === 'horde' ? 'HORDE' : 'ALLIANCE');

export const toRacials = (file: RacialsResponseFile): RacialsData => {
    const classRacials = Object.entries(file.classRacials ?? {});

    const races: Race[] = Object.entries(file.factions ?? {}).flatMap(([factionKey, factionRaces]) =>
        factionRaces.map(race => ({
            slug: slugify(race.race),
            name: race.race,
            faction: toFaction(factionKey),
            icon: race.icon || FALLBACK_ICON,
            classNames: race.classes ?? [],
            abilities: (race.abilities ?? []).map(toAbility),
            classRacials: classRacials.flatMap(([className, group]): ClassRacials[] => {
                const abilities = group.races?.[race.race] ?? [];

                return abilities.length > 0 ? [{ className, abilities: abilities.map(toAbility) }] : [];
            })
        }))
    );

    return { races, credit: toCredit(file.meta) };
};
