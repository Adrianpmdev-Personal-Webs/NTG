/**
 * SpellbookConfig
 *
 * Texts of the spellbook screen for the codes the data file uses.
 *
 * @author @adrianpmdev
 */

/** Name of the tab with the spells every character has. */
export const GENERAL_TAB_NAME = 'General';

/** Where a rank is learned, by the trainer code of the data (`nt`). */
export const LEARNED_FROM_LABELS: Readonly<Partial<Record<number, string>>> = {
    1: 'Lo enseña el entrenador de portales',
    2: 'Se aprende con un tomo, una misión o un botín',
    3: 'Se aprende con el talento',
    4: 'No lo vende el entrenador de clase'
};

export const LEARNED_FROM_QUEST = 'Lo enseña una misión de clase';
