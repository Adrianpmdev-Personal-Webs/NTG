/**
 * NavIcon
 *
 * The icons of the navigation menus, drawn as SVG lines in the color of the text, so they look the same on every
 * device. Its size comes from the styles of whoever uses it.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const PATHS = {
    home: 'M3 11l9-8 9 8M5 9.5V21h5v-6h4v6h5V9.5',
    dashboard: 'M3 21h18M6 21v-7M12 21V5M18 21V10',
    admin: 'M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6zM8.5 12l2.5 2.5 4.5-5',
    users: 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM2 21v-1a6 6 0 0 1 6-6h2a6 6 0 0 1 6 6v1M16 3.5a4 4 0 0 1 0 7.5M18 14.5a6 6 0 0 1 4 5.5v1',
    gamepad:
        'M6 8h12a4 4 0 0 1 4 4v3a3 3 0 0 1-5.4 1.8L15 15H9l-1.6 1.8A3 3 0 0 1 2 15v-3a4 4 0 0 1 4-4zM7 10.5v3M5.5 12h3M15.5 11h.01M18 13h.01',
    image: 'M3 5h18v14H3zM3 16l5-5 4 4 3-3 6 6M15.5 9.5h.01',
    music: 'M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM20 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z',
    play: 'M7 4l13 8-13 8z',
    trophy: 'M8 3h8v7a4 4 0 0 1-8 0zM8 5H4v2a4 4 0 0 0 4 4M16 5h4v2a4 4 0 0 1-4 4M12 14v4M8 21h8M10 18h4v3h-4z',
} as const;

export type NavIconName = keyof typeof PATHS;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-nav-icon',
    templateUrl: './nav-icon.html',
    styleUrl: './nav-icon.scss',
})
export class NavIcon {
    readonly name = input.required<NavIconName>();

    protected readonly path = computed(() => PATHS[this.name()]);
}
