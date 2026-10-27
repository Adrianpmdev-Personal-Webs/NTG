/**
 * CalendarView
 *
 * Series of a league or of one group, by round ("Jornada").
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Match, TournamentDetail } from '../../data/pokemon-models';
import { playersById, roundsOf } from '../../data/tournament-view';
import { MatchCard } from '../match-card/match-card';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-calendar-view',
    imports: [MatchCard],
    templateUrl: './calendar-view.html',
    styleUrl: './calendar-view.scss'
})
export class CalendarView {

    readonly detail = input.required<TournamentDetail>();
    /** Series to show (the league, or the series of one group). */
    readonly matches = input.required<Match[]>();
    readonly editable = input(false);

    readonly edit = output<Match>();

    readonly players = computed(() => playersById(this.detail()));
    readonly rounds = computed(() => {
        const stage = this.matches()[0]?.stage ?? 'league';

        return roundsOf(this.matches(), stage);
    });
}
