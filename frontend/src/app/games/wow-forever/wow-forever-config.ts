/**
 * WowForeverConfig
 *
 * Presentation helpers shared by the WoW Forever screens: the credit line of the game data and the fallback for icons
 * that cannot be loaded. The texts, tools, news and icon URLs come from the backend.
 *
 * @author @adrianpmdev
 */

import { DataCredit } from './game-data/game-data-models';

/**
 * Credit line the licence of the game data asks for, with the game build when the data gives it, e.g.
 * "Data from talentsforever.com (https://talentsforever.com) · Build 1.60.1.70425". Null while there is no data.
 */
export const creditLine = (credit: DataCredit | null): string | null => {
    if (!credit?.attribution) {
        return null;
    }

    return credit.build ? `${credit.attribution} · Build ${credit.build}` : credit.attribution;
};

/** Hides an icon image that could not be loaded; the name next to it still identifies the item. */
export const hideBrokenIcon = (event: Event): void => {
    (event.target as HTMLImageElement).style.visibility = 'hidden';
};
