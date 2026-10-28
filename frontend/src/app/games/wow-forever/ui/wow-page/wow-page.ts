/**
 * WowPage
 *
 * Shell of every WoW Forever tool page: the metal frame, a header with the way back to the launcher and to NTG, the
 * page title and, at the bottom, the credit line the game data asks for. The page content is projected in between.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { WowButton } from '../wow-button/wow-button';
import { WowFrame } from '../wow-frame/wow-frame';

@Component({
    selector: 'app-wow-page',
    imports: [WowButton, WowFrame],
    templateUrl: './wow-page.html',
    styleUrl: './wow-page.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WowPage {
    readonly title = input.required<string>();
    readonly credit = input<string | null>(null);
}
