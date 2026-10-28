/**
 * Notifications
 *
 * Global notifications component.
 * Displays active notifications managed by the notification service.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NotificationCenter } from '@core/notifications/notification-center';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-notifications',
    templateUrl: './notifications.html',
    styleUrl: './notifications.scss',
})
export class Notifications {
    protected readonly notificationCenter = inject(NotificationCenter);
}
