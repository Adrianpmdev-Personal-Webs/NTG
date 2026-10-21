/**
 * App
 *
 * Root component. Hosts the router outlet and the global notifications.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Notifications } from '@shared/components/notifications/notifications';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-root',
    imports: [RouterOutlet, Notifications],
    templateUrl: './app.html',
    styleUrl: './app.scss'
})
export class App {}
