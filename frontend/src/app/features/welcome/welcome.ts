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
import { AuthSession } from '@core/auth/auth-session';
import { GameCatalog } from '@core/games/game-catalog';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-welcome',
    imports: [RouterLink],
    templateUrl: './welcome.html',
    styleUrl: './welcome.scss',
})
export class Welcome implements OnInit {
    private readonly authSession = inject(AuthSession);
    private readonly catalog = inject(GameCatalog);

    protected readonly currentUser = toSignal(this.authSession.currentUser$, {
        initialValue: this.authSession.getCurrentUser(),
    });

    protected readonly games = this.catalog.games;
    protected readonly isAdmin = computed(() => {
        this.currentUser();

        return this.authSession.isAdmin();
    });

    protected readonly hasNoTags = computed(() => !this.isAdmin() && (this.currentUser()?.tags.length ?? 0) === 0);

    ngOnInit(): void {
        this.authSession.reloadUser();
        this.catalog.refresh();
    }
}
