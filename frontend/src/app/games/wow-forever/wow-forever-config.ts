/**
 * WowForeverConfig
 *
 * Presentation helpers shared by the WoW Forever screens: the credit line of the game data and the fallback for icons
 * that cannot be loaded. The texts, tools, news and icon URLs come from the backend.
 *
 * @author @adrianpmdev
 */

import { DataCredit } from './game-data/game-data-models';

export const creditLine = (credit: DataCredit | null): string | null => {
    if (!credit?.attribution) {
        return null;
    }

    return credit.build ? `${credit.attribution} · Build ${credit.build}` : credit.attribution;
};

export const hideBrokenIcon = (event: Event): void => {
    (event.target as HTMLImageElement).style.visibility = 'hidden';
};
