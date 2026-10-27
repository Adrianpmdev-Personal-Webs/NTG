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
import { ACCEPTED_IMAGE_TYPES, AssetService, checkImageFile } from '@core/services/asset';
import { GameCatalogService } from '@core/services/game-catalog';
import { NotificationService } from '@core/services/notification';
import { CatalogGame } from '@shared/models/game';
import { resolveApiError } from '@shared/utils/api-error';

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
    imports: [FormsModule],
    templateUrl: './games.html',
    styleUrl: './games.scss'
})
export class Games {

    private readonly catalog = inject(GameCatalogService);
    private readonly assetService = inject(AssetService);
    private readonly notifications = inject(NotificationService);

    readonly accepted = ACCEPTED_IMAGE_TYPES;
    readonly games = this.catalog.games;
    readonly loading = signal(true);
    readonly saving = signal(false);
    /** Key of the game whose cover is being uploaded. */
    readonly uploadingCover = signal<string | null>(null);
    readonly dialog = signal<Dialog>(null);

    form: GameForm = this.emptyForm();

    constructor() {
        this.catalog.load().pipe(catchError(error => {
            this.notifications.error(resolveApiError(error, 'No se han podido cargar los juegos'));
            return of([]);
        })).subscribe(() => this.loading.set(false));
    }

    openCreate(): void {
        this.form = this.emptyForm();
        this.dialog.set({ mode: 'create' });
    }

    openEdit(game: CatalogGame): void {
        this.form = { key: game.id, name: game.name, upcoming: game.upcoming, sortOrder: game.sortOrder };
        this.dialog.set({ mode: 'edit', game });
    }

    openDelete(game: CatalogGame): void {
        this.dialog.set({ mode: 'delete', game });
    }

    close(): void {
        this.dialog.set(null);
    }

    save(): void {
        const dialog = this.dialog();
        const request = { name: this.form.name, upcoming: this.form.upcoming, sortOrder: Number(this.form.sortOrder) };

        if (dialog?.mode === 'edit') {
            this.run(this.catalog.update(dialog.game.id, request), 'Juego actualizado');
        } else {
            this.run(this.catalog.create({ ...request, key: this.form.key.trim().toLowerCase() }), 'Juego creado');
        }
    }

    confirmDelete(): void {
        const dialog = this.dialog();

        if (dialog?.mode === 'delete') {
            this.run(this.catalog.delete(dialog.game.id), 'Juego eliminado del catálogo');
        }
    }

    uploadCover(game: CatalogGame, event: Event): void {
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
        this.assetService.upload(`platform/games/${game.id}`, file).subscribe({
            next: () => {
                this.uploadingCover.set(null);
                this.notifications.success(`Portada de ${game.name} guardada`);
                this.catalog.refresh();
            },
            error: error => {
                this.uploadingCover.set(null);
                this.notifications.error(resolveApiError(error, 'No se ha podido subir la portada'));
            }
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
            error: error => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar'));
            }
        });
    }

    private emptyForm(): GameForm {
        return { key: '', name: '', upcoming: true, sortOrder: this.catalog.games().length + 1 };
    }
}
