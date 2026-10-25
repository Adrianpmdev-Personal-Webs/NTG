/**
 * TalentsModels
 *
 * Models of the talent calculator. The first block describes talents.json as WoW Forever publishes it; the second one
 * is what the app works with (see TalentsApi, the only place that knows the file format).
 *
 * @author @adrianpmdev
 */

/* ─── talents.json (as published) ─── */

export interface TalentFile {
    id: number;
    name: string | null;
    row: number;
    column: number;
    max_ranks: number;
    points_required: number;
    requires: number[];
    spell_id: number;
    icon_id: number;
    description?: string[];
}

export interface TalentTreeFile {
    id: number;
    name: string;
    rows: number;
    columns: number;
    talents: TalentFile[];
}

export interface TalentClassFile {
    id: number;
    name: string;
    slug: string;
    trees: TalentTreeFile[];
}

export interface TalentsFile {
    build: string;
    attribution: string;
    levels_that_grant_a_point: number[];
    classes: TalentClassFile[];
}

/* ─── App models ─── */

export interface Talent {
    id: number;
    treeId: number;
    /** Null for a position the source keeps (so build codes still line up) but does not describe: it is not drawn. */
    name: string | null;
    /** Zero based, top row first. */
    row: number;
    /** Zero based, left column first. */
    column: number;
    /** Points the talent takes. 0 means it cannot be bought in this build. */
    maxRanks: number;
    /** Points that must already be spent in the rows above, in the same tree. */
    pointsRequired: number;
    /** Talents of the same tree and a row above; with several, any one of them at full rank is enough. */
    requires: number[];
    spellId: number;
    iconId: number;
    /** Text of each rank; one entry when every rank reads alike, none when the talent has no text. */
    descriptions: string[];
}

export interface TalentTree {
    id: number;
    name: string;
    rows: number;
    columns: number;
    /** In build code order: position N is digit N of the tree's part of a share code. */
    talents: Talent[];
}

/** A talent under the pointer (or keyboard focus), with the screen position the tooltip card hangs from. */
export interface TalentHover {
    talent: Talent;
    x: number;
    y: number;
}

export interface TalentClass {
    slug: string;
    trees: TalentTree[];
}

export interface TalentsData {
    /** Game build the talents come from. */
    build: string;
    /** Credit line the licence asks us to show. */
    attribution: string;
    /** Levels that give a talent point, in order. */
    levels: number[];
    classes: TalentClass[];
}
