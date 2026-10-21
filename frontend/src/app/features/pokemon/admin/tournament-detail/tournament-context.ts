/**
 * TournamentContextFacade
 *
 * Tournament context facade.
 * Manages current game and locke context for tournament detail pages.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { GameResponse, LockeResponse } from '../../pokemon-models';
import { PokemonApi } from '../../pokemon-api';

@Injectable({ providedIn: 'root' })
export class TournamentContextFacade {

    private readonly pokemonService = inject(PokemonApi);

    readonly gameId = signal('');
    readonly lockeId = signal('');
    readonly game = signal<GameResponse | null>(null);
    readonly locke = signal<LockeResponse | null>(null);

    initialize(route: ActivatedRoute): void {
        const gameId = route.snapshot.paramMap.get('gameId') || '';
        const lockeId = route.snapshot.paramMap.get('lockeId') || '';
        const state = history.state as { game?: GameResponse; locke?: LockeResponse };

        this.gameId.set(gameId);
        this.lockeId.set(lockeId);

        if (state.game) {
            this.game.set(state.game);
        } else if (gameId) {
            this.loadGame(gameId);
        }

        if (state.locke) {
            this.locke.set(state.locke);
        }
    }

    private loadGame(gameId: string): void {
        this.pokemonService.getGameById(gameId).subscribe({
            next: game => this.game.set(game)
        });
    }
}
