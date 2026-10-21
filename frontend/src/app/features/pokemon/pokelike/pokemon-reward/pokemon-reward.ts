/**
 * PokemonReward
 *
 * Pokelike pokemon reward component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-reward',
    imports: [],
    templateUrl: './pokemon-reward.html',
    styleUrl: './pokemon-reward.scss'
})
export class PokemonReward {

    readonly store = inject(PokelikeRunStore);
}
