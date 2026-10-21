/**
 * RunStatus
 *
 * Pokelike run status component.
 * Displays current phase and global run counters.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { GamePhase } from '../pokelike-models';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-run-status',
    imports: [],
    templateUrl: './run-status.html',
    styleUrl: './run-status.scss'
})
export class RunStatus {

    readonly store = inject(PokelikeRunStore);

    getPhaseTitle(): string {
        const titleMap: Record<GamePhase, string> = {
            home: 'NUEVA PARTIDA',
            starter: 'ELIGE STARTER',
            map: 'NEXT MAP',
            battle: 'WILD POKEMON APPEARED',
            pokemonReward: 'CHOOSE ONE POKEMON',
            itemReward: 'CHOOSE ONE ITEM',
            trade: 'TRADE OFFER',
            badge: 'BADGE EARNED',
            gameOver: 'GAME OVER',
            hallOfFame: 'HALL OF FAME'
        };

        return titleMap[this.store.currentPhase()];
    }
}
