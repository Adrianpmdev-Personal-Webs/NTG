/**
 * Welcome
 *
 * Welcome page for regular users. Greets the user and lists the games they have access to (by tag).
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { GAMES } from '@core/config/games';
import { AuthService } from '@core/services/auth';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-welcome',
    imports: [RouterLink],
    templateUrl: './welcome.html',
    styleUrl: './welcome.scss'
})
export class Welcome implements OnInit {

    private readonly authService = inject(AuthService);

    readonly currentUser = toSignal(this.authService.currentUser$, {
        initialValue: this.authService.getCurrentUser()
    });

    /** Upcoming games are shown to everyone (locked for players); released ones only with the matching tag. */
    readonly games = computed(() => {
        this.currentUser();

        return GAMES.filter(game => game.upcoming || this.authService.canAccessGame(game.id));
    });
    readonly isAdmin = computed(() => {
        this.currentUser();

        return this.authService.isAdmin();
    });

    /** Only players with no tag at all are told to ask an admin; the others may just have games not released yet. */
    readonly hasNoTags = computed(() => !this.isAdmin() && (this.currentUser()?.tags.length ?? 0) === 0);

    ngOnInit(): void {
        // Tags may have changed since login, so the list is refreshed when the user comes home.
        this.authService.reloadUser();
    }
}
