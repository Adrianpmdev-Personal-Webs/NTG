/**
 * WowButton
 *
 * Metal launcher button with an optional large variant. Content is projected as the label. With `link` it is a router link
 * (an anchor) that looks exactly the same.
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
})
export class WowButton {
    readonly large = input(false);
    readonly link = input<string>();
    readonly disabled = input(false);
}
