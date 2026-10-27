/**
 * LegacyConfig
 *
 * Texts of the Legacy System screen that the data file gives in English, and where the challenge checklist is kept.
 *
 * @author @adrianpmdev
 */

/** Spanish name of each challenge group; a group not listed shows the name the data gives it. */
export const CHALLENGE_GROUP_LABELS: Readonly<Partial<Record<string, string>>> = {
    Classes: 'Clases',
    Crafting: 'Profesiones',
    'Dungeons and raids': 'Mazmorras y bandas',
    PvP: 'JcJ',
    Other: 'Otros'
};

/** Name shown for a slot the game keeps for a perk still to come. */
export const PLACEHOLDER_PERK_NAME = 'Próximamente';

/** Browser storage key of the challenges the player has ticked. It only lives in this browser. */
export const COMPLETED_CHALLENGES_KEY = 'ntg.wow-forever.legacy.completed';
