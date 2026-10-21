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

    /** Upcoming games are shown to everyone; playable ones only when the user has the matching tag. */
    readonly games = computed(() => {
        this.currentUser();

        return GAMES.filter(game => game.upcoming || this.authService.canAccessGame(game.id));
    });
    readonly hasPlayableGames = computed(() => this.games().some(game => !game.upcoming));

    ngOnInit(): void {
        // Tags may have changed since login, so the list is refreshed when the user comes home.
        this.authService.reloadUser();
    }
}
