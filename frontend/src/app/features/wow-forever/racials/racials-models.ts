/**
 * RacialsModels
 *
 * Models of the races screen: every race with its faction, the classes it can play and its racial abilities.
 *
 * @author @adrianpmdev
 */

import { Ability, DataCredit } from '../game-data/game-data-models';
import { Faction } from '../wow-forever-models';

/** Abilities of one class that change with the race (some priest spells, for example). */
export interface ClassRacials {
    className: string;
    abilities: Ability[];
}

export interface Race {
    slug: string;
    name: string;
    faction: Faction;
    icon: string;
    /** Names of the classes it can play, as the class list writes them. */
    classNames: string[];
    abilities: Ability[];
    classRacials: ClassRacials[];
}

export interface RacialsData {
    races: Race[];
    credit: DataCredit;
}
