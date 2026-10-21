/**
 * BattleView
 *
 * Pokelike battle view component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-battle-view',
    imports: [],
    templateUrl: './battle-view.html',
    styleUrl: './battle-view.scss'
})
export class BattleView {

    readonly store = inject(PokelikeRunStore);
}
