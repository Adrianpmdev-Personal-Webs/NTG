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
import { GameAdminItem } from '@core/config/game-module';
import { AuthService } from '@core/services/auth';
import { GameCatalogService } from '@core/services/game-catalog';
import { GAME_MODULES } from '@games/games';
import { Footer } from '../footer/footer';
import { Header } from '../header/header';

/** A game of the sidebar: catalog data plus the links this web client has for it. */
interface SidebarGame {
    id: string;
    name: string;
    upcoming: boolean;
    /** Player route, or null when this client has no module for the game yet. */
    route: string | null;
    adminRoute: string;
    adminItems: readonly GameAdminItem[];
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'terminal' },
    selector: 'app-main-layout',
    imports: [RouterOutlet, RouterLink, RouterLinkActive, Header, Footer],
    templateUrl: './main-layout.html',
    styleUrl: './main-layout.scss'
})
export class MainLayout {

    readonly authService = inject(AuthService);
    private readonly catalog = inject(GameCatalogService);

    readonly sidebarCollapsed = signal(false);
    readonly openGameId = signal<string | null>(null);
    readonly currentUser = toSignal(this.authService.currentUser$, {
        initialValue: this.authService.getCurrentUser()
    });

    /** Games the user can open: players the released ones they have the tag for, admins all (upcoming marked with *). */
    readonly games = computed<SidebarGame[]>(() => this.catalog.games()
        .filter(game => game.accessible)
        .map(game => {
            const module = GAME_MODULES.find(entry => entry.id === game.id);

            return {
                id: game.id,
                name: game.name,
                upcoming: game.upcoming,
                route: module ? `/${game.id}` : null,
                adminRoute: `/admin/${game.id}`,
                adminItems: module?.adminItems ?? []
            };
        }));

    private readonly router = inject(Router);
    private readonly mobileSidebarBreakpoint = '(max-width: 900px)';
    private readonly currentUrl = toSignal(
        this.router.events.pipe(
            filter((event): event is NavigationEnd => event instanceof NavigationEnd),
            map(event => event.urlAfterRedirects)
        ),
        { initialValue: this.router.url }
    );

    constructor() {
        if (this.isMobileSidebarLayout()) {
            this.sidebarCollapsed.set(true);
        }

        // The catalog fills the games signal; a failure just leaves the sidebar without games.
        this.catalog.load().pipe(catchError(() => of([]))).subscribe();

        // Keeps open the submenu of the game whose admin area is being visited.
        effect(() => {
            const activeGame = this.games().find(game => this.isGameActive(game));

            this.openGameId.set(activeGame?.id ?? null);
        });
    }

    toggleSidebar(): void {
        this.sidebarCollapsed.update(isCollapsed => !isCollapsed);
    }

    closeSidebar(): void {
        this.sidebarCollapsed.set(true);
    }

    toggleSubmenu(gameId: string): void {
        this.openGameId.update(openId => (openId === gameId ? null : gameId));
    }

    isSubmenuOpen(gameId: string): boolean {
        return this.openGameId() === gameId;
    }

    isGameActive(game: SidebarGame): boolean {
        return this.currentUrl().split('?')[0].startsWith(game.adminRoute);
    }

    handleMenuItemNavigation(): void {
        this.openGameId.set(null);
        this.closeSidebarOnMobile();
    }

    handleSubmenuItemNavigation(): void {
        this.closeSidebarOnMobile();
    }

    closeSidebarOnMobile(): void {
        if (this.isMobileSidebarLayout()) {
            this.closeSidebar();
        }
    }

    logout(): void {
        this.authService.logout();
    }

    private isMobileSidebarLayout(): boolean {
        return typeof window !== 'undefined' && window.matchMedia(this.mobileSidebarBreakpoint).matches;
    }
}
