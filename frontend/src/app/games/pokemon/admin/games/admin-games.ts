/**
 * AdminGames
 *
 * Management of the Pokémon games: list, create, edit and delete. A game with tournaments cannot be
 * deleted (the API answers with a message).
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { resolveApiError } from '@shared/utils/api-error';
import { PokemonApi } from '../../data/pokemon-api';
import { Game, GameRequest } from '../../data/pokemon-models';
import { ModalOverlay } from '@shared/directives/modal-overlay';

type Dialog = { mode: 'create' } | { mode: 'edit'; game: Game } | { mode: 'delete'; game: Game } | null;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-admin-games',
    imports: [ModalOverlay, FormsModule],
    templateUrl: './admin-games.html',
})
export class AdminGames {
    private readonly api = inject(PokemonApi);
    private readonly notifications = inject(NotificationCenter);

    protected readonly games = signal<Game[]>([]);
    protected readonly loading = signal(true);
    protected readonly saving = signal(false);
    protected readonly dialog = signal<Dialog>(null);

    protected form: GameRequest = { name: '', description: '', imageUrl: '' };

    constructor() {
        this.load();
    }

    private load(): void {
        this.api.getGames().subscribe({
            next: (games) => {
                this.games.set(games);
                this.loading.set(false);
            },
            error: (error) => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se han podido cargar los juegos'));
            },
        });
    }

    protected openCreate(): void {
        this.form = { name: '', description: '', imageUrl: '' };
        this.dialog.set({ mode: 'create' });
    }

    protected openEdit(game: Game): void {
        this.form = { name: game.name, description: game.description ?? '', imageUrl: game.imageUrl };
        this.dialog.set({ mode: 'edit', game });
    }

    protected openDelete(game: Game): void {
        this.dialog.set({ mode: 'delete', game });
    }

    protected close(): void {
        this.dialog.set(null);
    }

    protected save(): void {
        const dialog = this.dialog();
        const request: GameRequest = { ...this.form, description: this.form.description?.trim() || null };
        const call =
            dialog?.mode === 'edit' ? this.api.updateGame(dialog.game.id, request) : this.api.createGame(request);

        this.run(call, dialog?.mode === 'edit' ? 'Juego actualizado' : 'Juego creado');
    }

    protected confirmDelete(): void {
        const dialog = this.dialog();

        if (dialog?.mode === 'delete') {
            this.run(this.api.deleteGame(dialog.game.id), 'Juego eliminado');
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
