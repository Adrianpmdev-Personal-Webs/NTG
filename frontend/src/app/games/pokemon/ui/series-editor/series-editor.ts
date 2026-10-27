/**
 * SeriesEditor
 *
 * Modal where an admin records the games of a best-of-3 series: the winner and the replay of each game.
 * It only offers the games that can still be played (the series ends when a side wins two).
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Match, MatchGameInput, TournamentPlayer } from '../../data/pokemon-models';

const WINS_NEEDED = 2;
const MAX_GAMES = 3;

interface GameDraft {
    winnerSide: 1 | 2 | null;
    replayUrl: string;
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-series-editor',
    imports: [FormsModule],
    templateUrl: './series-editor.html'
})
export class SeriesEditor {

    readonly match = input.required<Match>();
    readonly players = input.required<Map<string, TournamentPlayer>>();
    readonly saving = input(false);
    /** Offer to delete the series (not for the series of a generated bracket). */
    readonly removable = input(false);

    readonly save = output<MatchGameInput[]>();
    readonly cancel = output<void>();
    readonly remove = output<void>();

    readonly games = signal<GameDraft[]>([]);

    /** Rows shown: the recorded games plus the next one while the series is open. */
    readonly visibleGames = computed(() => {
        const rows: GameDraft[] = [];
        let wins1 = 0;
        let wins2 = 0;

        for (const game of this.games()) {
            rows.push(game);

            if (game.winnerSide === 1) {
                wins1++;
            } else if (game.winnerSide === 2) {
                wins2++;
            } else {
                break;
            }

            if (wins1 === WINS_NEEDED || wins2 === WINS_NEEDED) {
                break;
            }
        }

        return rows;
    });

    readonly score = computed(() => {
        const decided = this.visibleGames().filter(game => game.winnerSide !== null);

        return [decided.filter(game => game.winnerSide === 1).length, decided.filter(game => game.winnerSide === 2).length];
    });

    constructor() {
        effect(() => {
            const recorded: GameDraft[] = this.match().games.map(game => ({ winnerSide: game.winnerSide, replayUrl: game.replayUrl ?? '' }));
            const empty = Array.from({ length: MAX_GAMES - recorded.length }, () => ({ winnerSide: null, replayUrl: '' }));
            this.games.set([...recorded, ...empty]);
        });
    }

    name(side: 1 | 2): string {
        const id = side === 1 ? this.match().player1Id : this.match().player2Id;

        return this.players().get(id ?? '')?.name ?? '—';
    }

    setWinner(index: number, side: 1 | 2): void {
        this.games.update(games => games.map((game, i) => (i === index ? { ...game, winnerSide: side } : game)));
    }

    setReplay(index: number, replayUrl: string): void {
        this.games.update(games => games.map((game, i) => (i === index ? { ...game, replayUrl } : game)));
    }

    clear(): void {
        this.games.set(Array.from({ length: MAX_GAMES }, () => ({ winnerSide: null, replayUrl: '' })));
    }

    submit(): void {
        const games = this.visibleGames()
            .filter((game): game is GameDraft & { winnerSide: 1 | 2 } => game.winnerSide !== null)
            .map(game => ({ winnerSide: game.winnerSide, replayUrl: game.replayUrl.trim() || null }));

        this.save.emit(games);
    }
}
