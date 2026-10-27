/**
 * TalentsConfig
 *
 * Rules of the talent calculator that the data file does not carry: when points are earned, how many open each row
 * and the id of every tree in the share codes.
 *
 * @author @adrianpmdev
 */

/** Levels that give a talent point: one per level from 10 to 60, 51 points in total. */
export const TALENT_POINT_LEVELS: readonly number[] = Array.from({ length: 51 }, (_, index) => index + 10);

/** Points that open each row of a talent tree: row 2 needs 5 points in the rows above, row 3 needs 10, and so on. */
export const POINTS_PER_TALENT_ROW = 5;

/**
 * Id of each tree in the share codes, by `<class>-<tree>`. They are the game's own ids (the ones wow-forever.gg uses),
 * so a build code keeps the same format. A tree that is not listed gets its position in the class (1, 2 or 3).
 */
export const TALENT_TREE_IDS: Readonly<Partial<Record<string, number>>> = {
    'warrior-arms': 26,
    'warrior-fury': 256,
    'warrior-protection': 257,
    'paladin-holy': 594,
    'paladin-protection': 267,
    'paladin-retribution': 184,
    'hunter-beast-mastery': 50,
    'hunter-marksmanship': 163,
    'hunter-survival': 51,
    'rogue-assassination': 253,
    'rogue-combat': 38,
    'rogue-subtlety': 39,
    'priest-discipline': 613,
    'priest-holy': 56,
    'priest-shadow': 78,
    'shaman-elemental': 375,
    'shaman-enhancement': 373,
    'shaman-restoration': 374,
    'mage-arcane': 237,
    'mage-fire': 8,
    'mage-frost': 6,
    'warlock-affliction': 355,
    'warlock-demonology': 354,
    'warlock-destruction': 593,
    'druid-balance': 574,
    'druid-feral-combat': 134,
    'druid-restoration': 573
};
