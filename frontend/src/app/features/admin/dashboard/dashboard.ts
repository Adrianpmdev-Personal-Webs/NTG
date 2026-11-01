/**
 * Dashboard
 *
 * Admin dashboard, with real data only: the memory and CPU of the backend, the space used in Supabase, the state each
 * module declares, the accounts with their last activity, and the traffic and latest warnings and errors since the
 * last start. It is read when the screen opens and with the refresh
 * button, never on its own, so a forgotten tab costs nothing; between reads only the uptime advances, in the browser.
 * A value the backend could not read shows as a dash.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NotificationCenter } from '@core/notifications/notification-center';
import { NavIcon, NavIconName } from '@shared/components/nav-icon/nav-icon';
import { resolveApiError } from '@shared/utils/api-error';
import { DashboardApi, DashboardData } from './dashboard-api';

const WARNING_PERCENT = 70;
const DANGER_PERCENT = 90;
const CLOCK_INTERVAL_MS = 30_000;
const BUCKET_ICONS: Readonly<Record<string, NavIconName>> = { 'public-music': 'music', 'public-assets': 'image' };

interface UsageCard {
    label: string;
    value: string;
    detail: string;
    percent: number | null;
    icon: NavIconName;
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-dashboard',
    imports: [DatePipe, NavIcon],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss',
})
export class Dashboard {
    private readonly api = inject(DashboardApi);
    private readonly notifications = inject(NotificationCenter);

    protected readonly data = signal<DashboardData | null>(null);
    protected readonly loading = signal(true);
    protected readonly readAt = signal<Date | null>(null);
    private readonly now = signal(Date.now());

    protected readonly uptime = computed(() => {
        const seconds = this.data()?.server.uptimeSeconds;
        const readAt = this.readAt();

        if (seconds === undefined || !readAt) {
            return '—';
        }

        return formatUptime(seconds + Math.max(0, Math.floor((this.now() - readAt.getTime()) / 1_000)));
    });

    protected readonly cards = computed<UsageCard[]>(() => {
        const data = this.data();

        if (!data) {
            return [];
        }

        const { server, storage } = data;
        const memory = server.processBytes ?? server.heapUsedBytes;
        const memoryLimit = server.processBytes === null ? server.heapMaxBytes : server.memoryLimitBytes;

        return [
            usage(server.processBytes === null ? 'HEAP JAVA' : 'MEMORIA RENDER', 'memory', memory, memoryLimit),
            {
                label: 'CPU',
                value: server.cpuPercent === null ? '—' : `${Math.round(server.cpuPercent)}%`,
                detail: 'en este momento',
                percent: server.cpuPercent,
                icon: 'cpu',
            },
            usage('BASE DE DATOS', 'database', storage.databaseBytes, storage.databaseLimitBytes),
            usage('STORAGE', 'storage', storage.storageBytes, storage.storageLimitBytes),
        ];
    });

    constructor() {
        this.refresh();

        const clock = setInterval(() => this.now.set(Date.now()), CLOCK_INTERVAL_MS);

        inject(DestroyRef).onDestroy(() => clearInterval(clock));
    }

    protected refresh(): void {
        this.loading.set(true);
        this.api.get().subscribe({
            next: (data) => {
                this.data.set(data);
                this.readAt.set(new Date());
                this.now.set(Date.now());
                this.loading.set(false);
            },
            error: (error) => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido cargar el dashboard'));
            },
        });
    }

    protected size(bytes: number): string {
        return formatBytes(bytes);
    }

    protected share(part: number, whole: number): number {
        return whole > 0 ? (part / whole) * 100 : 0;
    }

    protected usageColor(percent: number | null): string {
        if (percent !== null && percent >= DANGER_PERCENT) {
            return 'var(--neon-red)';
        }

        return percent !== null && percent >= WARNING_PERCENT ? 'var(--neon-yellow)' : 'var(--neon-green)';
    }

    protected bucketIcon(bucket: string): NavIconName {
        return BUCKET_ICONS[bucket] ?? 'folder';
    }

    protected isAdminRole(role: string): boolean {
        return role.toLowerCase().replace('role_', '') === 'admin';
    }
}

function usage(label: string, icon: NavIconName, used: number | null, limit: number): UsageCard {
    return {
        label,
        icon,
        value: used === null ? '—' : formatBytes(used),
        detail: `de ${formatBytes(limit)}`,
        percent: used === null || limit <= 0 ? null : (used / limit) * 100,
    };
}

function formatBytes(bytes: number): string {
    const megabytes = bytes / 1024 / 1024;

    if (megabytes >= 1024) {
        return `${(megabytes / 1024).toFixed(1)} GB`;
    }

    if (megabytes >= 100) {
        return `${Math.round(megabytes)} MB`;
    }

    return megabytes >= 1 ? `${megabytes.toFixed(1)} MB` : `${Math.round(bytes / 1024)} KB`;
}

function formatUptime(seconds: number): string {
    const days = Math.floor(seconds / 86_400);
    const hours = Math.floor((seconds % 86_400) / 3_600);
    const minutes = Math.floor((seconds % 3_600) / 60);

    return `${days > 0 ? `${days}d ` : ''}${hours}h ${String(minutes).padStart(2, '0')}m`;
}
