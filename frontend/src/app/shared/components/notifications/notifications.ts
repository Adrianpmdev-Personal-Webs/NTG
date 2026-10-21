/**
 * Notifications
 *
 * Global notifications component.
 * Displays active notifications managed by the notification service.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NotificationService } from '@core/services/notification';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-notifications',
    templateUrl: './notifications.html',
    styleUrl: './notifications.scss',
})
export class Notifications {
    readonly notificationService = inject(NotificationService);
}
