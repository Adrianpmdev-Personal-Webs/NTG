/**
 * LastVisit
 *
 * Remembers in this browser the last screen of a game each user opened, so the home page can offer to continue there.
 * Only the routes of a game that name themselves with `data: { visitLabel: '...' }` count: the entry page of a game is
 * already one click away from the home page, so it never replaces something more useful. It costs no call, and
 * whether the game can still be opened is checked by the home page against the catalog (and by the guards anyway).
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthSession } from '@core/auth/auth-session';

const STORAGE_KEY = 'ntg.lastVisit';

export interface Visit {
    userId: string;
    gameId: string;
    label: string;
    path: string;
}

@Injectable({ providedIn: 'root' })
export class LastVisit {
    private readonly router = inject(Router);
    private readonly authSession = inject(AuthSession);

    constructor() {
        this.router.events
            .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
            .subscribe((event) => this.record(event.urlAfterRedirects));
    }

    forCurrentUser(): Visit | null {
        const visit = readStored();

        return visit && visit.userId === this.authSession.getCurrentUser()?.id ? visit : null;
    }

    private record(url: string): void {
        const label = labelOf(this.router.routerState.snapshot.root);
        const userId = this.authSession.getCurrentUser()?.id;
        const path = url.split(/[?#]/)[0];
        const gameId = path.split('/').filter(Boolean)[0];

        if (label && userId && gameId) {
            writeStored({ userId, gameId, label, path });
        }
    }
}

function labelOf(root: ActivatedRouteSnapshot): string | null {
    let route = root;

    while (route.firstChild) {
        route = route.firstChild;
    }

    const label: unknown = route.data['visitLabel'];

    return typeof label === 'string' ? label : null;
}

function readStored(): Visit | null {
    try {
        const parsed: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null');

        return isVisit(parsed) ? parsed : null;
    } catch {
        return null;
    }
}

function writeStored(visit: Visit): void {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(visit));
    } catch {
        return;
    }
}

function isVisit(value: unknown): value is Visit {
    const visit = value as Partial<Visit> | null;

    return (
        typeof visit?.userId === 'string' &&
        typeof visit.gameId === 'string' &&
        typeof visit.label === 'string' &&
        typeof visit.path === 'string' &&
        visit.path.startsWith('/')
    );
}
