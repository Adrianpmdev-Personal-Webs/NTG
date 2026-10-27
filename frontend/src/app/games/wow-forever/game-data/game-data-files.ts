/**
 * GameDataFiles
 *
 * Shape of the responses of the game data API. Each one carries a section of data.json (the talentsforever.com
 * export) exactly as it is in the file, plus a `meta` block with the credit. Only GameDataMappers reads these types:
 * the rest of the app works with the models of each tool. Fields a hand-edited file might leave out are optional.
 *
 * @author @adrianpmdev
 */

/* ─── Shared ─── */

export interface DataMetaFile {
    license?: string;
    attribution?: string;
    /** Day the export was generated, e.g. 2026-10-26. */
    generated?: string;
}

export interface DataClassFile {
    slug: string;
    name: string;
    icon: string;
}

/** `[name, text, icon]`, as racials and class highlights are written in the file. */
export type AbilityFile = [name: string, text: string, icon: string];

export interface DataIndexFile {
    meta?: DataMetaFile;
    classes?: DataClassFile[];
}

/* ─── Talents ─── */

export type ClassicStatusFile = 'same' | 'moved' | 'changed' | 'new' | 'note';

export interface TalentFile {
    name: string;
    /** Rank cap. */
    max: number;
    /** One based, top row first. */
    row: number;
    /** One based, left column first. */
    col: number;
    icon: string;
    /** Text of each rank, rank 1 first. */
    desc?: string[];
    /** Name of the talent of the same tree that must be at full rank first. */
    req?: string;
    /** Cost line of an active talent, e.g. "15 Rage | Melee Range | Instant". */
    cost?: string;
    passive?: boolean;
    classic?: { status?: ClassicStatusFile };
}

export interface TalentTreeFile {
    name: string;
    icon: string;
    talents?: TalentFile[];
}

export interface ClassTalentsFile {
    icon?: string;
    /** Where the trees were read from, with the game build. */
    source?: string;
    trees?: TalentTreeFile[];
}

export interface TalentsResponseFile {
    meta?: DataMetaFile;
    class: DataClassFile;
    talents: ClassTalentsFile;
}

/* ─── Spellbook ─── */

/** `[spell name, rank]`; the rank is empty for spells with a single rank. */
export type SpellRefFile = [name: string, rank: string];

export interface SpellbookFile {
    general?: SpellRefFile[];
    tabs?: { name: string; spells?: SpellRefFile[] }[];
    /** Level each rank is learned at, by spell name. */
    levels?: Record<string, number[]>;
    /** Spells a talent puts in the book. */
    talents?: string[];
    /** Icon by spell name. */
    icons?: Record<string, string>;
    /** Classic spells this class no longer has. */
    gone?: string[];
    build?: string;
}

export interface SpellTextFile {
    /** Tooltip lines, each one with a left and a right part (cost and range, cast time and cooldown...). */
    l?: [string, string][];
    /** Description. */
    d?: string;
    /** "Learned at level N". */
    lv?: string;
    /** Spell id. */
    id?: number;
    /** School (Physical, Holy, Fire...). */
    sc?: string;
    /** Spell power coefficient. */
    co?: string;
    /** Comparison with Classic. */
    cs?: ClassicStatusFile;
    /** Classic description. */
    cd?: string;
    /** 1 portal trainer, 2 tome, quest or drop, 3 comes with the talent, 4 not sold by the class trainer. */
    nt?: number;
    /** "quest" when a class quest teaches it. */
    ns?: string;
    /** Extra note. */
    fx?: string;
}

export interface SpellbookResponseFile {
    meta?: DataMetaFile;
    class: DataClassFile;
    spellbook: SpellbookFile;
    /** Text of each rank, keyed `<spell>|<rank>`. */
    spells?: Record<string, SpellTextFile>;
    highlights?: AbilityFile[];
}

/* ─── Racials ─── */

export interface RaceFile {
    race: string;
    icon: string;
    /** Names of the classes the race can play. */
    classes?: string[];
    abilities?: AbilityFile[];
}

export interface ClassRacialsFile {
    note?: string;
    /** Abilities of the class that depend on the race, by race name. */
    races?: Record<string, AbilityFile[]>;
}

export interface RacialsResponseFile {
    meta?: DataMetaFile;
    /** Races by faction name ("Horde", "Alliance"). */
    factions?: Record<string, RaceFile[]>;
    /** By class name. */
    classRacials?: Record<string, ClassRacialsFile>;
}

/* ─── Legacy ─── */

export interface PerkFile {
    name: string;
    max: number;
    row: number;
    col: number;
    icon: string;
    ranks?: string[];
    /** Points that must already be spent in the tree. */
    gate?: number;
    /** Name of the perk that must be at full rank first. */
    req?: string;
    /** A slot kept for a perk still to come. */
    placeholder?: boolean;
    cost?: string;
}

export interface PerkTreeFile {
    id: number;
    name: string;
    icon: string;
    perks?: PerkFile[];
}

export interface ChallengeFile {
    group: string;
    name: string;
    desc?: string;
    points?: number;
}

export interface LegacyFile {
    note?: string;
    /** Points every character can spend. */
    points?: number;
    source?: string;
    trees?: PerkTreeFile[];
    challenges?: ChallengeFile[];
}

export interface LegacyResponseFile {
    meta?: DataMetaFile;
    legacy: LegacyFile;
}
