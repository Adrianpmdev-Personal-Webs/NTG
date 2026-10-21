/**
 * ItemReward
 *
 * Pokelike item reward component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-item-reward',
    imports: [],
    templateUrl: './item-reward.html',
    styleUrl: './item-reward.scss'
})
export class ItemReward {

    readonly store = inject(PokelikeRunStore);
}
