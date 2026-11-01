/**
 * App
 *
 * Root component. Hosts the router outlet, the global notifications and the background music control, and starts
 * remembering the last screen of a game each user opens.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LastVisit } from '@core/games/last-visit';
import { MusicControl } from '@shared/components/music-control/music-control';
import { Notifications } from '@shared/components/notifications/notifications';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-root',
    imports: [RouterOutlet, Notifications, MusicControl],
    templateUrl: './app.html',
    styleUrl: './app.scss',
})
export class App {
    constructor() {
        inject(LastVisit);
    }
}
