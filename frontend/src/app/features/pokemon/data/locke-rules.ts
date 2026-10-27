/**
 * locke-rules
 *
 * Rules document of a randomlocke (stored as JSON by the backend), and the helpers the admin editor uses
 * to turn it into text and back.
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

/** Pretty JSON of a rules document, or an empty text when there are none. */
export function rulesToText(rules: LockeRules | null): string {
    return rules ? JSON.stringify(rules, null, 2) : '';
}

/**
 * Reads the rules typed in the editor.
 *
 * @returns the document, null for an empty text, or undefined when the text is not a valid document
 */
export function parseRules(text: string): LockeRules | null | undefined {
    if (!text.trim()) {
        return null;
    }

    try {
        const value: unknown = JSON.parse(text);
        const valid = typeof value === 'object' && value !== null && !Array.isArray(value)
            && (!('rules' in value) || Array.isArray((value as LockeRules).rules));

        return valid ? value as LockeRules : undefined;
    } catch {
        return undefined;
    }
}
