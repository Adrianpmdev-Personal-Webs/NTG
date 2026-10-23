/**
 * WowButton
 *
 * Metal launcher button with an optional large variant. Content is projected as the label.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
    selector: 'app-wow-button',
    templateUrl: './wow-button.html',
    styleUrl: './wow-button.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class WowButton {

    readonly large = input(false);
    readonly disabled = input(false);
}
