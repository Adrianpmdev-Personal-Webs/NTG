/**
 * WowPage
 *
 * Shell of every WoW Forever tool page: the metal frame, a header with the way back to the launcher and to NTG, the
 * page title and, at the bottom, the credit line the game data asks for. The page content is projected in between.
 * With `compactNav`, below the tablet breakpoint the two way-back buttons move into a menu at the left of the header.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { WowButton } from '../wow-button/wow-button';
import { WowFrame } from '../wow-frame/wow-frame';
import { WowNavLink, WowNavMenu } from '../wow-nav-menu/wow-nav-menu';

@Component({
    selector: 'app-wow-page',
    imports: [WowButton, WowFrame, WowNavMenu],
    templateUrl: './wow-page.html',
    styleUrl: './wow-page.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WowPage {
    readonly heading = input.required<string>();
    readonly credit = input<string | null>(null);
    readonly compactNav = input(false);

    protected readonly links: readonly WowNavLink[] = [
        { label: '← Lanzador', link: '/wow-forever' },
        { label: '← NTG', link: '/' },
    ];
}
