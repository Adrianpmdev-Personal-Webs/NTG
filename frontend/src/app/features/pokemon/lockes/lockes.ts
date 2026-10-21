/**
 * Lockes
 *
 * Read-only list of the lockes grouped by game, with their rules and a link to each locke's tournaments.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PokemonApi } from '../pokemon-api';
import { GameResponse, LockeResponse } from '../pokemon-models';

interface GameLockes {
    game: GameResponse;
    lockes: LockeResponse[];
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-locke-list',
    imports: [RouterLink],
    templateUrl: './lockes.html',
    styleUrl: './lockes.scss'
})
export class Lockes {

    private readonly pokemonApi = inject(PokemonApi);

    readonly loading = signal(true);
    readonly failed = signal(false);
    readonly groups = signal<GameLockes[]>([]);

    constructor() {
        forkJoin({
            games: this.pokemonApi.getAllGames(),
            lockes: this.pokemonApi.getAllLockes()
        }).subscribe({
            next: ({ games, lockes }) => {
                this.groups.set(
                    games
                        .map(game => ({ game, lockes: lockes.filter(locke => locke.gameId === game.id) }))
                        .filter(group => group.lockes.length > 0)
                );
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            }
        });
    }
}
