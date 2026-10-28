/**
 * GamePage
 *
 * One game and the randomlockes that have tournaments in it.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, switchMap } from 'rxjs';
import { PokemonApi } from '../data/pokemon-api';
import { Game, LockeSummary } from '../data/pokemon-models';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-game',
    imports: [RouterLink],
    templateUrl: './game.html',
    styleUrl: './game.scss',
})
export class GamePage {
    private readonly api = inject(PokemonApi);

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly game = signal<Game | null>(null);
    protected readonly lockes = signal<LockeSummary[]>([]);

    constructor() {
        inject(ActivatedRoute)
            .paramMap.pipe(
                switchMap((params) => {
                    const id = params.get('gameId') ?? '';
                    this.loading.set(true);

                    return forkJoin({ game: this.api.getGame(id), lockes: this.api.getLockes(id) });
                }),
            )
            .subscribe({
                next: ({ game, lockes }) => {
                    this.game.set(game);
                    this.lockes.set(lockes);
                    this.failed.set(false);
                    this.loading.set(false);
                },
                error: () => {
                    this.failed.set(true);
                    this.loading.set(false);
                },
            });
    }

    protected gameNames(locke: LockeSummary): string {
        return locke.games.map((game) => game.name).join(' → ');
    }
}
