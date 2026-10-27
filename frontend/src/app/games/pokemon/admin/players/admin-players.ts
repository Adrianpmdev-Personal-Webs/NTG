/**
 * AdminPlayers
 *
 * Management of the players of the randomlockes. A player can be linked to a platform account: that is
 * what lets them upload their own team pictures. A player who has played a tournament cannot be deleted.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, forkJoin } from 'rxjs';
import { NotificationService } from '@core/services/notification';
import { UserService } from '@core/services/user';
import { User } from '@shared/models/user';
import { resolveApiError } from '@shared/utils/api-error';
import { PokemonApi } from '../../data/pokemon-api';
import { Player, PlayerRequest } from '../../data/pokemon-models';

type Dialog = { mode: 'create' } | { mode: 'edit'; player: Player } | { mode: 'delete'; player: Player } | null;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-admin-players',
    imports: [FormsModule],
    templateUrl: './admin-players.html'
})
export class AdminPlayers {

    private readonly api = inject(PokemonApi);
    private readonly userService = inject(UserService);
    private readonly notifications = inject(NotificationService);

    readonly players = signal<Player[]>([]);
    readonly users = signal<User[]>([]);
    readonly loading = signal(true);
    readonly saving = signal(false);
    readonly dialog = signal<Dialog>(null);

    /** Accounts that already belong to a player (an account links to one player only). */
    readonly linkedAccounts = computed(() => new Set(this.players().map(player => player.userId).filter(Boolean)));

    form: PlayerRequest = { name: '', userId: null };

    constructor() {
        this.load();
    }

    load(): void {
        forkJoin({ players: this.api.getPlayers(), users: this.userService.getAllUsers() }).subscribe({
            next: ({ players, users }) => {
                this.players.set(players);
                this.users.set([...users].sort((a, b) => a.username.localeCompare(b.username)));
                this.loading.set(false);
            },
            error: error => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se han podido cargar los jugadores'));
            }
        });
    }

    accountName(userId: string | null): string {
        return userId ? this.users().find(user => user.id === userId)?.username ?? 'Cuenta desconocida' : '—';
    }

    /** Accounts offered in the form: free ones plus the one already linked to this player. */
    availableUsers(current: string | null): User[] {
        return this.users().filter(user => user.id === current || !this.linkedAccounts().has(user.id));
    }

    openCreate(): void {
        this.form = { name: '', userId: null };
        this.dialog.set({ mode: 'create' });
    }

    openEdit(player: Player): void {
        this.form = { name: player.name, userId: player.userId };
        this.dialog.set({ mode: 'edit', player });
    }

    openDelete(player: Player): void {
        this.dialog.set({ mode: 'delete', player });
    }

    close(): void {
        this.dialog.set(null);
    }

    save(): void {
        const dialog = this.dialog();
        const request: PlayerRequest = { name: this.form.name, userId: this.form.userId || null };

        this.run(
            dialog?.mode === 'edit' ? this.api.updatePlayer(dialog.player.id, request) : this.api.createPlayer(request),
            dialog?.mode === 'edit' ? 'Jugador actualizado' : 'Jugador creado'
        );
    }

    confirmDelete(): void {
        const dialog = this.dialog();

        if (dialog?.mode === 'delete') {
            this.run(this.api.deletePlayer(dialog.player.id), 'Jugador eliminado');
        }
    }

    private run(call: Observable<unknown>, message: string): void {
        this.saving.set(true);
        call.subscribe({
            next: () => {
                this.saving.set(false);
                this.dialog.set(null);
                this.notifications.success(message);
                this.load();
            },
            error: error => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar'));
            }
        });
    }
}
