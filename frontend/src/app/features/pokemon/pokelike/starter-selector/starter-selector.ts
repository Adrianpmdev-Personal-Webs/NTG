/**
 * StarterSelector
 *
 * Pokelike starter selector component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-starter-selector',
    imports: [],
    templateUrl: './starter-selector.html',
    styleUrl: './starter-selector.scss'
})
export class StarterSelector {

    readonly store = inject(PokelikeRunStore);
}
