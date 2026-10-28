/**
 * WowButton
 *
 * Metal launcher button with an optional large variant and an optional `block` variant that fills its cell below the
 * tablet breakpoint (its label may then wrap). Content is projected as the label; `label` names it for screen readers
 * when the visible text is not enough.
 * With `link` it is a router link (an anchor) that looks exactly the same.
 *
 * @author @adrianpmdev
 */

import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-wow-button',
    imports: [NgTemplateOutlet, RouterLink],
    templateUrl: './wow-button.html',
    styleUrl: './wow-button.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { '[class.wow-button-host--block]': 'block()' },
})
export class WowButton {
    readonly large = input(false);
    readonly block = input(false);
    readonly link = input<string>();
    readonly disabled = input(false);
    readonly label = input<string>();
}
