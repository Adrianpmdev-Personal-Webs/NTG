/**
 * ProfessionsModels
 *
 * Models of the professions screen: the professions by group, each with what it is for, the profession it goes with
 * and the first camp feature it unlocks (item, skill it needs, buff in a few words and the description of the game).
 * Every text is in English.
 *
 * @author @adrianpmdev
 */

import { DataCredit } from '../game-data/game-data-models';

export interface CampFeature {
    name: string;
    icon: string;
    skill: number;
    effect: string;
    description: string;
}

export interface Profession {
    slug: string;
    name: string;
    icon: string;
    description: string;
    pairsWith: string | null;
    camp: CampFeature | null;
}

export interface ProfessionGroup {
    id: string;
    name: string;
    note: string | null;
    icon: string;
    professions: Profession[];
}

export interface ProfessionsData {
    groups: ProfessionGroup[];
    credit: DataCredit;
}
