/**
 * App
 *
 * Root component. Hosts the router outlet, the screen shown until the first page is ready, the global notifications and
 * the background music control, and starts remembering the last screen of a game each user opens.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, NavigationError, Router, RouterOutlet } from '@angular/router';
import { filter, take } from 'rxjs';
import { LastVisit } from '@core/games/last-visit';
import { LoadingScreen } from '@shared/components/loading-screen/loading-screen';
import { MusicControl } from '@shared/components/music-control/music-control';
import { Notifications } from '@shared/components/notifications/notifications';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-root',
    imports: [RouterOutlet, LoadingScreen, Notifications, MusicControl],
    templateUrl: './app.html',
    styleUrl: './app.scss',
})
export class App {
    protected readonly starting = signal(true);

    constructor() {
        inject(LastVisit);
        inject(Router)
            .events.pipe(
                filter((event) => event instanceof NavigationEnd || event instanceof NavigationError),
                take(1),
                takeUntilDestroyed(),
            )
            .subscribe(() => this.starting.set(false));
    }
}
