/**
 * ClassPicker
 *
 * Row of class buttons (icon and name) used by the tools that show one class at a time.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { GameClass } from '../../game-data/game-data-models';
import { hideBrokenIcon } from '../../wow-forever-config';

@Component({
    selector: 'app-class-picker',
    templateUrl: './class-picker.html',
    styleUrl: './class-picker.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ClassPicker {

    readonly classes = input.required<readonly GameClass[]>();
    /** Slug of the class shown, or null when none is selected yet. */
    readonly selected = input<string | null>(null);

    readonly pick = output<GameClass>();

    readonly onIconError = hideBrokenIcon;
}
