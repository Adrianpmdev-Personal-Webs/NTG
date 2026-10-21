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
import { filter, map } from 'rxjs';
import { GAMES, GameEntry } from '@core/config/games';
import { AuthService } from '@core/services/auth';
import { Footer } from '../footer/footer';
import { Header } from '../header/header';

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

    readonly sidebarCollapsed = signal(false);
    readonly openGameId = signal<string | null>(null);
    readonly currentUser = toSignal(this.authService.currentUser$, {
        initialValue: this.authService.getCurrentUser()
    });

    /** Playable games the user can open (admins see all of them). */
    readonly games = computed(() => {
        this.currentUser();

        return GAMES.filter(game => !game.upcoming && this.authService.canAccessGame(game.id));
    });

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

    isGameActive(game: GameEntry): boolean {
        return !!game.adminRoute && this.currentUrl().split('?')[0].startsWith(game.adminRoute);
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
