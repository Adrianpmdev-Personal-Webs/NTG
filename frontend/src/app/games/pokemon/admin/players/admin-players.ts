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
import { NotificationCenter } from '@core/notifications/notification-center';
import { UserApi } from '@core/users/user-api';
import { User } from '@shared/models/user';
import { resolveApiError } from '@shared/utils/api-error';
import { PokemonApi } from '../../data/pokemon-api';
import { Player, PlayerRequest } from '../../data/pokemon-models';
import { ModalOverlay } from '@shared/directives/modal-overlay';

type Dialog = { mode: 'create' } | { mode: 'edit'; player: Player } | { mode: 'delete'; player: Player } | null;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-admin-players',
    imports: [ModalOverlay, FormsModule],
    templateUrl: './admin-players.html',
})
export class AdminPlayers {
    private readonly api = inject(PokemonApi);
    private readonly userApi = inject(UserApi);
    private readonly notifications = inject(NotificationCenter);

    protected readonly players = signal<Player[]>([]);
    private readonly users = signal<User[]>([]);
    protected readonly loading = signal(true);
    protected readonly saving = signal(false);
    protected readonly dialog = signal<Dialog>(null);

    private readonly linkedAccounts = computed(
        () =>
            new Set(
                this.players()
                    .map((player) => player.userId)
                    .filter(Boolean),
            ),
    );

    protected form: PlayerRequest = { name: '', userId: null };

    constructor() {
        this.load();
    }

    private load(): void {
        forkJoin({ players: this.api.getPlayers(), users: this.userApi.getAllUsers() }).subscribe({
            next: ({ players, users }) => {
                this.players.set(players);
                this.users.set([...users].sort((a, b) => a.username.localeCompare(b.username)));
                this.loading.set(false);
            },
            error: (error) => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se han podido cargar los jugadores'));
            },
        });
    }

    protected accountName(userId: string | null): string {
        return userId ? (this.users().find((user) => user.id === userId)?.username ?? 'Cuenta desconocida') : '—';
    }

    protected availableUsers(current: string | null): User[] {
        return this.users().filter((user) => user.id === current || !this.linkedAccounts().has(user.id));
    }

    protected openCreate(): void {
        this.form = { name: '', userId: null };
        this.dialog.set({ mode: 'create' });
    }

    protected openEdit(player: Player): void {
        this.form = { name: player.name, userId: player.userId };
        this.dialog.set({ mode: 'edit', player });
    }

    protected openDelete(player: Player): void {
        this.dialog.set({ mode: 'delete', player });
    }

    protected close(): void {
        this.dialog.set(null);
    }

    protected save(): void {
        const dialog = this.dialog();
        const request: PlayerRequest = { name: this.form.name, userId: this.form.userId || null };

        this.run(
            dialog?.mode === 'edit' ? this.api.updatePlayer(dialog.player.id, request) : this.api.createPlayer(request),
            dialog?.mode === 'edit' ? 'Jugador actualizado' : 'Jugador creado',
        );
    }

    protected confirmDelete(): void {
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
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar'));
            },
        });
    }
}
