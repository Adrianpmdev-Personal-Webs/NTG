/**
 * NotificationService
 *
 * Service for managing global notifications in the admin panel.
 * Allows showing success, error, warning and info messages with auto-dismiss behavior.
 *
 * @author @adrianpmdev
 */

import { Injectable, signal } from '@angular/core';


export interface Notification {
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    message: string;
    leaving?: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {

    private readonly defaultDurationMs = 4_000;
    private readonly errorDurationMs = 5_000;
    private readonly exitAnimationDurationMs = 400;

    notifications = signal<Notification[]>([]);

    show(message: string, type: Notification['type'] = 'info', duration = this.defaultDurationMs): void {
        const id = Date.now().toString();
        const notification: Notification = {
            id,
            type,
            message
        };

        this.notifications.update(notifications => [...notifications, notification]);
        setTimeout(() => this.dismiss(id), duration);
    }

    success(message: string): void {
        this.show(message, 'success');
    }

    error(message: string): void {
        this.show(message, 'error', this.errorDurationMs);
    }

    warning(message: string): void {
        this.show(message, 'warning');
    }

    info(message: string): void {
        this.show(message, 'info');
    }

    dismiss(id: string): void {
        this.notifications.update(notifications =>
            notifications.map(notification =>
                notification.id === id ? { ...notification, leaving: true } : notification
            )
        );

        setTimeout(() => {
            this.notifications.update(notifications =>
                notifications.filter(notification => notification.id !== id)
            );
        }, this.exitAnimationDurationMs);
    }
}
