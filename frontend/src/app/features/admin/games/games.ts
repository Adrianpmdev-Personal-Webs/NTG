/**
 * Games
 *
 * Admin screen of the game catalog: name, order, whether a game is still coming soon and its cover. Releasing a game
 * is unticking "coming soon" here: players with its tag can open it right away (the backend opens its API too).
 * The key is the id of the game and of its tag, and it cannot change.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, catchError, of } from 'rxjs';
import { ACCEPTED_IMAGE_TYPES, AssetApi, checkImageFile } from '@core/assets/asset-api';
import { GameCatalog } from '@core/games/game-catalog';
import { NotificationCenter } from '@core/notifications/notification-center';
import { CatalogGame } from '@shared/models/game';
import { resolveApiError } from '@shared/utils/api-error';
import { ModalOverlay } from '@shared/directives/modal-overlay';

type Dialog = { mode: 'create' } | { mode: 'edit'; game: CatalogGame } | { mode: 'delete'; game: CatalogGame } | null;

interface GameForm {
    key: string;
    name: string;
    upcoming: boolean;
    sortOrder: number;
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-admin-games',
    imports: [ModalOverlay, FormsModule],
    templateUrl: './games.html',
    styleUrl: './games.scss',
})
export class Games {
    private readonly catalog = inject(GameCatalog);
    private readonly assetApi = inject(AssetApi);
    private readonly notifications = inject(NotificationCenter);

    protected readonly accepted = ACCEPTED_IMAGE_TYPES;
    protected readonly games = this.catalog.games;
    protected readonly loading = signal(true);
    protected readonly saving = signal(false);
    protected readonly uploadingCover = signal<string | null>(null);
    protected readonly dialog = signal<Dialog>(null);

    protected form: GameForm = this.emptyForm();

    constructor() {
        this.catalog
            .load()
            .pipe(
                catchError((error) => {
                    this.notifications.error(resolveApiError(error, 'No se han podido cargar los juegos'));
                    return of([]);
                }),
            )
            .subscribe(() => this.loading.set(false));
    }

    protected openCreate(): void {
        this.form = this.emptyForm();
        this.dialog.set({ mode: 'create' });
    }

    protected openEdit(game: CatalogGame): void {
        this.form = { key: game.id, name: game.name, upcoming: game.upcoming, sortOrder: game.sortOrder };
        this.dialog.set({ mode: 'edit', game });
    }

    protected openDelete(game: CatalogGame): void {
        this.dialog.set({ mode: 'delete', game });
    }

    protected close(): void {
        this.dialog.set(null);
    }

    protected save(): void {
        const dialog = this.dialog();
        const request = { name: this.form.name, upcoming: this.form.upcoming, sortOrder: Number(this.form.sortOrder) };

        if (dialog?.mode === 'edit') {
            this.run(this.catalog.update(dialog.game.id, request), 'Juego actualizado');
        } else {
            this.run(this.catalog.create({ ...request, key: this.form.key.trim().toLowerCase() }), 'Juego creado');
        }
    }

    protected confirmDelete(): void {
        const dialog = this.dialog();

        if (dialog?.mode === 'delete') {
            this.run(this.catalog.delete(dialog.game.id), 'Juego eliminado del catálogo');
        }
    }

    protected uploadCover(game: CatalogGame, event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        input.value = '';

        if (!file) {
            return;
        }

        const problem = checkImageFile(file);

        if (problem) {
            this.notifications.warning(problem);
            return;
        }

        this.uploadingCover.set(game.id);
        this.assetApi.upload(`platform/games/${game.id}`, file).subscribe({
            next: () => {
                this.uploadingCover.set(null);
                this.notifications.success(`Portada de ${game.name} guardada`);
                this.catalog.refresh();
            },
            error: (error) => {
                this.uploadingCover.set(null);
                this.notifications.error(resolveApiError(error, 'No se ha podido subir la portada'));
            },
        });
    }

    private run(call: Observable<unknown>, message: string): void {
        this.saving.set(true);
        call.subscribe({
            next: () => {
                this.saving.set(false);
                this.dialog.set(null);
                this.notifications.success(message);
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar'));
            },
        });
    }

    private emptyForm(): GameForm {
        return { key: '', name: '', upcoming: true, sortOrder: this.catalog.games().length + 1 };
    }
}
