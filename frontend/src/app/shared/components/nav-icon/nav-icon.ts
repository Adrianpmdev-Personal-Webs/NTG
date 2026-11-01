/**
 * NavIcon
 *
 * The icons of the menus and the dashboard, drawn as SVG lines in the color of the text, so they look the same on every
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
    user: 'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21v-1a7 7 0 0 1 7-7h2a7 7 0 0 1 7 7v1',
    userOff: 'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21v-1a7 7 0 0 1 7-7h2a7 7 0 0 1 7 7v1M3 3l18 18',
    mail: 'M3 6h18v13H3zM3 7l9 7 9-7',
    memory: 'M3 7h18v9H3zM7 16v3M12 16v3M17 16v3M7 10v3M12 10v3M17 10v3',
    cpu: 'M7 7h10v10H7zM10 10h4v4h-4zM10 4v3M14 4v3M10 17v3M14 17v3M4 10h3M4 14h3M17 10h3M17 14h3',
    database:
        'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3',
    storage: 'M7 18a4 4 0 0 1-.6-8A6 6 0 0 1 18 9a4.5 4.5 0 0 1-.5 9z',
    folder: 'M3 6h6l2 3h10v10H3z',
    package: 'M12 3l8 4.5v9L12 21l-8-4.5v-9zM4 7.5l8 4.5 8-4.5M12 12v9',
    calendar: 'M4 6h16v15H4zM4 10h16M8 3v4M16 3v4',
    clock: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2',
    alert: 'M12 3l10 18H2zM12 10v5M12 18h.01',
    download: 'M12 3v12M7 11l5 5 5-5M4 20h16',
    refresh: 'M20 11a8 8 0 0 0-14-4.5L4 9M4 4v5h5M4 13a8 8 0 0 0 14 4.5L20 15M20 20v-5h-5',
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
