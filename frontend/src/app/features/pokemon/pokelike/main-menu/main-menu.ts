/**
 * MainMenu
 *
 * Pokelike main menu component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-main-menu',
    imports: [],
    templateUrl: './main-menu.html',
    styleUrl: './main-menu.scss'
})
export class MainMenu {

    readonly store = inject(PokelikeRunStore);
}
