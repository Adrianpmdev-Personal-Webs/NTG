/**
 * InventoryPanel
 *
 * Pokelike inventory panel component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-inventory-panel',
    imports: [],
    templateUrl: './inventory-panel.html',
    styleUrl: './inventory-panel.scss'
})
export class InventoryPanel {

    readonly store = inject(PokelikeRunStore);
}
