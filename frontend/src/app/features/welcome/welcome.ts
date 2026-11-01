/**
 * Welcome
 *
 * Home page. Greets the user, offers to continue on the last screen of a game they opened (remembered by this browser)
 * and shows every game as a large cover, with what is happening in it when the game tells. The highlights are one call
 * when the page opens; without them the page simply shows the covers.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { AuthSession } from '@core/auth/auth-session';
import { GameCatalog } from '@core/games/game-catalog';
import { LastVisit } from '@core/games/last-visit';
import { NavIcon } from '@shared/components/nav-icon/nav-icon';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-welcome',
    imports: [RouterLink, NgTemplateOutlet, NavIcon],
    templateUrl: './welcome.html',
    styleUrl: './welcome.scss',
})
export class Welcome implements OnInit {
    private readonly authSession = inject(AuthSession);
    private readonly catalog = inject(GameCatalog);
    private readonly lastVisit = inject(LastVisit);

    protected readonly currentUser = toSignal(this.authSession.currentUser$, {
        initialValue: this.authSession.getCurrentUser(),
    });

    protected readonly games = this.catalog.games;
    protected readonly highlights = signal<Readonly<Record<string, string>>>({});

    protected readonly continueVisit = computed(() => {
        this.currentUser();

        const visit = this.lastVisit.forCurrentUser();
        const game = this.games().find((entry) => entry.id === visit?.gameId && entry.accessible);

        return visit && game ? { gameName: game.name, label: visit.label, path: visit.path } : null;
    });

    protected readonly isAdmin = computed(() => {
        this.currentUser();

        return this.authSession.isAdmin();
    });

    protected readonly hasNoTags = computed(() => !this.isAdmin() && (this.currentUser()?.tags.length ?? 0) === 0);

    ngOnInit(): void {
        this.authSession.reloadUser();
        this.catalog.refresh();
        this.catalog.getHighlights().subscribe({
            next: (highlights) =>
                this.highlights.set(Object.fromEntries(highlights.map((entry) => [entry.game, entry.text]))),
            error: () => this.highlights.set({}),
        });
    }
}
