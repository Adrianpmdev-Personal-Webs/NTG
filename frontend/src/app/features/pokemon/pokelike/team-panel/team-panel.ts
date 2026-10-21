/**
 * TeamPanel
 *
 * Pokelike team panel component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-team-panel',
    imports: [],
    templateUrl: './team-panel.html',
    styleUrl: './team-panel.scss'
})
export class TeamPanel {

    readonly store = inject(PokelikeRunStore);
}
