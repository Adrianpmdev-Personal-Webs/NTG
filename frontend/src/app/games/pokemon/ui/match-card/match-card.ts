/**
 * MatchCard
 *
 * One best-of-3 series: both players, the score, its state and a link to the replay of every game.
 * When editable, clicking it asks the parent to edit its games.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MATCH_STATUS_LABELS } from '../../data/pokemon-labels';
import { Match, TournamentPlayer } from '../../data/pokemon-models';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-match-card',
    templateUrl: './match-card.html',
    styleUrl: './match-card.scss',
})
export class MatchCard {
    readonly match = input.required<Match>();
    readonly players = input.required<Map<string, TournamentPlayer>>();
    readonly editable = input(false);

    readonly edit = output<Match>();

    protected readonly statusLabel = computed(() => MATCH_STATUS_LABELS[this.match().status]);

    protected readonly canEdit = computed(
        () => this.editable() && !!this.match().player1Id && !!this.match().player2Id,
    );

    protected name(playerId: string | null): string {
        if (!playerId) {
            return this.match().status === 'bye' || this.match().status === 'void' ? '—' : 'Por decidir';
        }

        return this.players().get(playerId)?.name ?? '—';
    }

    protected isMine(playerId: string | null): boolean {
        return !!playerId && !!this.players().get(playerId)?.mine;
    }

    protected editMatch(): void {
        if (this.canEdit()) {
            this.edit.emit(this.match());
        }
    }
}
