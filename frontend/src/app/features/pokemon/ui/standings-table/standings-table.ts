/**
 * StandingsTable
 *
 * Standings table used for tournaments, groups, randomlockes and the global ranking. Styled with the
 * terminal tokens, which the Pokémon layout maps to its own palette.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { StandingRow } from '../../data/tournament-view';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-standings-table',
    templateUrl: './standings-table.html',
    styleUrl: './standings-table.scss'
})
export class StandingsTable {

    readonly rows = input.required<StandingRow[]>();

    /** Header of the extra column, or null to hide it. */
    readonly extraLabel = input<string | null>(null);

    /** Positions highlighted as podium. */
    readonly podium = input(3);
}
