/**
 * AdminLockes
 *
 * Management of the randomlockes: list, create, edit (name, description and rules) and delete. Deleting
 * a randomlocke deletes its tournaments and their team pictures. Its tournaments and roster are managed
 * in its own screen.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NotificationService } from '@core/services/notification';
import { resolveApiError } from '@shared/utils/api-error';
import { parseRules, rulesToText } from '../../data/locke-rules';
import { PokemonApi } from '../../data/pokemon-api';
import { LockeSummary } from '../../data/pokemon-models';

type Dialog = { mode: 'create' } | { mode: 'edit'; id: string } | { mode: 'delete'; locke: LockeSummary } | null;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-admin-lockes',
    imports: [FormsModule, RouterLink],
    templateUrl: './admin-lockes.html'
})
export class AdminLockes {

    private readonly api = inject(PokemonApi);
    private readonly notifications = inject(NotificationService);
    private readonly router = inject(Router);

    readonly lockes = signal<LockeSummary[]>([]);
    readonly loading = signal(true);
    readonly saving = signal(false);
    readonly dialog = signal<Dialog>(null);

    form = { name: '', description: '', rules: '' };

    constructor() {
        this.load();
    }

    load(): void {
        this.api.getLockes().subscribe({
            next: lockes => {
                this.lockes.set(lockes);
                this.loading.set(false);
            },
            error: error => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se han podido cargar los randomlockes'));
            }
        });
    }

    gameNames(locke: LockeSummary): string {
        return locke.games.map(game => game.name).join(', ') || '—';
    }

    openCreate(): void {
        this.form = { name: '', description: '', rules: '' };
        this.dialog.set({ mode: 'create' });
    }

    openEdit(summary: LockeSummary): void {
        // The list has no rules: read the full randomlocke first.
        this.api.getLocke(summary.id).subscribe({
            next: locke => {
                this.form = { name: locke.name, description: locke.description ?? '', rules: rulesToText(locke.rules) };
                this.dialog.set({ mode: 'edit', id: locke.id });
            },
            error: error => this.notifications.error(resolveApiError(error, 'No se ha podido cargar el randomlocke'))
        });
    }

    openDelete(locke: LockeSummary): void {
        this.dialog.set({ mode: 'delete', locke });
    }

    close(): void {
        this.dialog.set(null);
    }

    save(): void {
        const dialog = this.dialog();
        const rules = parseRules(this.form.rules);

        if (rules === undefined) {
            this.notifications.error('Las reglas no son un JSON válido');
            return;
        }

        const request = { name: this.form.name, description: this.form.description.trim() || null, rules };
        const call = dialog?.mode === 'edit' ? this.api.updateLocke(dialog.id, request) : this.api.createLocke(request);

        this.saving.set(true);
        call.subscribe({
            next: locke => {
                this.saving.set(false);
                this.dialog.set(null);
                this.notifications.success(dialog?.mode === 'edit' ? 'Randomlocke actualizado' : 'Randomlocke creado');

                if (dialog?.mode === 'create') {
                    this.router.navigate(['/admin/pokemon/lockes', locke.id]);
                } else {
                    this.load();
                }
            },
            error: error => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar'));
            }
        });
    }

    confirmDelete(): void {
        const dialog = this.dialog();

        if (dialog?.mode !== 'delete') {
            return;
        }

        this.saving.set(true);
        this.api.deleteLocke(dialog.locke.id).subscribe({
            next: () => {
                this.saving.set(false);
                this.dialog.set(null);
                this.notifications.success('Randomlocke eliminado');
                this.load();
            },
            error: error => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido eliminar'));
            }
        });
    }
}
