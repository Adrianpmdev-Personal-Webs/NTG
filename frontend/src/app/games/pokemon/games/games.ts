/**
 * Games
 *
 * Player home of the Pokémon module: the games we have played, each one with its picture, description
 * and the number of randomlockes played in it.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PokemonApi } from '../data/pokemon-api';
import { Game } from '../data/pokemon-models';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-games',
    imports: [RouterLink],
    templateUrl: './games.html',
    styleUrl: './games.scss',
})
export class Games {
    private readonly api = inject(PokemonApi);

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly games = signal<Game[]>([]);

    constructor() {
        this.api.getGames().subscribe({
            next: (games) => {
                this.games.set(games);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }
}
