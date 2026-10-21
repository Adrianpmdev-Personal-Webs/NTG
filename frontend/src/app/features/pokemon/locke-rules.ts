/**
 * locke-rules
 *
 * Locke rules models.
 * Defines the structure used to display locke rule sets.
 *
 * @author @adrianpmdev
 */

export interface LockeLevelCap {
    battle: string;
    level: number;
}

export interface LockeTournamentRule {
    name: string;
    level: number;
    rewards?: string[];
}

export interface LockeRule {
    number: number;
    title: string;
    description: string;
    conditions?: string[];
    exceptions?: string[];
    levelCaps?: LockeLevelCap[];
    tournaments?: LockeTournamentRule[];
    extra?: string;
}

export interface LockeRules {
    name?: string;
    rules?: LockeRule[];
}