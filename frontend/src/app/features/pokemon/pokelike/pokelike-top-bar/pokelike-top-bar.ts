/**
 * PokelikeTopBar
 *
 * Pokelike top bar component.
 * Displays image-based game navigation controls.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokelike-top-bar',
    imports: [],
    templateUrl: './pokelike-top-bar.html',
    styleUrl: './pokelike-top-bar.scss'
})
export class PokelikeTopBar {

    readonly store = inject(PokelikeRunStore);
}
