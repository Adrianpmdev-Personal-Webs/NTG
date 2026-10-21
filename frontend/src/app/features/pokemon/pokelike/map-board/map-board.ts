/**
 * MapBoard
 *
 * Pokelike map board component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-map-board',
    imports: [],
    templateUrl: './map-board.html',
    styleUrl: './map-board.scss'
})
export class MapBoard {

    readonly store = inject(PokelikeRunStore);
}
