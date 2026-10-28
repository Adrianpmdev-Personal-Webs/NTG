/**
 * BracketView
 *
 * Double-elimination bracket: the upper side, the lower side and the grand final, one column per round.
 * Series that nobody plays (void) are hidden.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { roundLabel } from '../../data/pokemon-labels';
import { Match, MatchStage, TournamentDetail } from '../../data/pokemon-models';
import { playersById, RoundView, roundsOf } from '../../data/tournament-view';
import { MatchCard } from '../match-card/match-card';

interface BracketSide {
    stage: MatchStage;
    title: string;
    rounds: (RoundView & { label: string })[];
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-bracket-view',
    imports: [MatchCard],
    templateUrl: './bracket-view.html',
    styleUrl: './bracket-view.scss',
})
export class BracketView {
    readonly detail = input.required<TournamentDetail>();
    readonly editable = input(false);

    readonly edit = output<Match>();

    protected readonly players = computed(() => playersById(this.detail()));

    protected readonly sides = computed<BracketSide[]>(() => {
        const matches = this.detail().matches.filter((match) => match.status !== 'void');
        const sides: BracketSide[] = [
            { stage: 'upper', title: 'Cuadro superior', rounds: [] },
            { stage: 'lower', title: 'Cuadro inferior', rounds: [] },
            { stage: 'grand_final', title: 'Gran final', rounds: [] },
        ];

        for (const side of sides) {
            const rounds = roundsOf(matches, side.stage);
            const last = Math.max(0, ...rounds.map((round) => round.round));
            side.rounds = rounds.map((round) => ({ ...round, label: roundLabel(side.stage, round.round, last) }));
        }

        return sides.filter((side) => side.rounds.length > 0);
    });
}
