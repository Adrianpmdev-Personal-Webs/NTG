/**
 * EndPanel
 *
 * Pokelike end panel component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-end-panel',
    imports: [],
    templateUrl: './end-panel.html',
    styleUrl: './end-panel.scss'
})
export class EndPanel {

    readonly store = inject(PokelikeRunStore);
}
