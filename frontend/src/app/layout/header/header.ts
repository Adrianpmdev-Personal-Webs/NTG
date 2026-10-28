/**
 * Header
 *
 * Top bar of the terminal layout. Shows the current path as a shell prompt, a live clock
 * and the button that opens the sidebar on small screens.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-header',
    templateUrl: './header.html',
    styleUrl: './header.scss',
})
export class Header {
    readonly menuToggle = output<void>();

    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);
    private readonly clockRefreshIntervalMs = 1_000;

    private readonly currentUrl = toSignal(
        this.router.events.pipe(
            filter((event): event is NavigationEnd => event instanceof NavigationEnd),
            map((event) => event.urlAfterRedirects),
        ),
        { initialValue: this.router.url },
    );

    protected readonly currentPath = computed(
        () =>
            this.currentUrl()
                .split('?')[0]
                .split('/')
                .filter(Boolean)
                .map((pathPart) => pathPart.toUpperCase())
                .join(' / ') || 'INICIO',
    );
    protected readonly currentTime = signal(this.formatTime());

    constructor() {
        const intervalId = setInterval(() => this.currentTime.set(this.formatTime()), this.clockRefreshIntervalMs);

        this.destroyRef.onDestroy(() => clearInterval(intervalId));
    }

    private formatTime(): string {
        return new Date().toLocaleTimeString('es-ES', { hour12: false });
    }
}
