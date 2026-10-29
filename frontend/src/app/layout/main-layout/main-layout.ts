/**
 * MainLayout
 *
 * Terminal-style shell for the general areas (welcome and admin). Holds the sidebar and its state,
 * with the header on top and the footer below the routed content.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, filter, map, of } from 'rxjs';
import { GameAdminItem } from '@core/games/game-module';
import { AuthSession } from '@core/auth/auth-session';
import { GameCatalog } from '@core/games/game-catalog';
import { GAME_MODULES } from '@games/games';
import { Footer } from '../footer/footer';
import { Header } from '../header/header';

interface SidebarGame {
    id: string;
    name: string;
    iconUrl: string | null;
    upcoming: boolean;
    route: string | null;
    adminRoute: string;
    adminItems: readonly GameAdminItem[];
    hasLinks: boolean;
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'terminal' },
    selector: 'app-main-layout',
    imports: [RouterOutlet, RouterLink, RouterLinkActive, Header, Footer],
    templateUrl: './main-layout.html',
    styleUrl: './main-layout.scss',
})
export class MainLayout {
    protected readonly authSession = inject(AuthSession);
    private readonly catalog = inject(GameCatalog);

    protected readonly sidebarCollapsed = signal(false);
    private readonly openGameId = signal<string | null>(null);
    protected readonly currentUser = toSignal(this.authSession.currentUser$, {
        initialValue: this.authSession.getCurrentUser(),
    });

    protected readonly games = computed<SidebarGame[]>(() =>
        this.catalog
            .games()
            .filter((game) => game.accessible)
            .map((game) => {
                const module = GAME_MODULES.find((entry) => entry.id === game.id);
                const adminItems = module?.adminItems ?? [];

                return {
                    id: game.id,
                    name: game.name,
                    iconUrl: game.iconUrl,
                    upcoming: game.upcoming,
                    route: module ? `/${game.id}` : null,
                    adminRoute: `/admin/${game.id}`,
                    adminItems,
                    hasLinks: !!module || (this.authSession.isAdmin() && adminItems.length > 0),
                };
            }),
    );

    private readonly router = inject(Router);
    private readonly mobileSidebarBreakpoint = '(max-width: 900px)';
    private readonly currentUrl = toSignal(
        this.router.events.pipe(
            filter((event): event is NavigationEnd => event instanceof NavigationEnd),
            map((event) => event.urlAfterRedirects),
        ),
        { initialValue: this.router.url },
    );

    constructor() {
        if (this.isMobileSidebarLayout()) {
            this.sidebarCollapsed.set(true);
        }

        this.catalog
            .load()
            .pipe(catchError(() => of([])))
            .subscribe();

        effect(() => {
            const activeGame = this.games().find((game) => this.isGameActive(game));

            this.openGameId.set(activeGame?.id ?? null);
        });
    }

    protected toggleSidebar(): void {
        this.sidebarCollapsed.update((isCollapsed) => !isCollapsed);
    }

    protected closeSidebar(): void {
        this.sidebarCollapsed.set(true);
    }

    protected toggleSubmenu(gameId: string): void {
        this.openGameId.update((openId) => (openId === gameId ? null : gameId));
    }

    protected isSubmenuOpen(gameId: string): boolean {
        return this.openGameId() === gameId;
    }

    protected isGameActive(game: SidebarGame): boolean {
        return this.currentUrl().split('?')[0].startsWith(game.adminRoute);
    }

    protected handleMenuItemNavigation(): void {
        this.openGameId.set(null);
        this.closeSidebarOnMobile();
    }

    protected handleSubmenuItemNavigation(): void {
        this.closeSidebarOnMobile();
    }

    private closeSidebarOnMobile(): void {
        if (this.isMobileSidebarLayout()) {
            this.closeSidebar();
        }
    }

    protected logout(): void {
        this.authSession.logout();
    }

    private isMobileSidebarLayout(): boolean {
        return typeof window !== 'undefined' && window.matchMedia(this.mobileSidebarBreakpoint).matches;
    }
}
