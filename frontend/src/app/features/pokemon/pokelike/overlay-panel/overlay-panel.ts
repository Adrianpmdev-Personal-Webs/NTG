/**
 * OverlayPanel
 *
 * Pokelike overlay panel component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-overlay-panel',
    imports: [],
    templateUrl: './overlay-panel.html',
    styleUrl: './overlay-panel.scss'
})
export class OverlayPanel {

    readonly store = inject(PokelikeRunStore);
}
