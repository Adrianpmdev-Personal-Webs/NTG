/**
 * LegacyModels
 *
 * Models of the Legacy System: the perk trees (talent trees with tree-wide point gates) and the account challenges
 * that earn the points.
 *
 * @author @adrianpmdev
 */

import { DataCredit } from '../game-data/game-data-models';
import { TalentTree } from '../talents/talents-models';

export interface LegacyChallenge {
    name: string;
    description: string;
    points: number;
}

export interface ChallengeGroup {
    /** Group name as the data writes it (Classes, Crafting...). */
    name: string;
    challenges: LegacyChallenge[];
    points: number;
}

export interface LegacyData {
    /** Points every character can spend in its trees. */
    points: number;
    trees: TalentTree[];
    challengeGroups: ChallengeGroup[];
    /** Points all the challenges give together. */
    challengePoints: number;
    credit: DataCredit;
}
