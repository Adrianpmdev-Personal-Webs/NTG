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
import { NotificationCenter } from '@core/notifications/notification-center';
import { resolveApiError } from '@shared/utils/api-error';
import { parseRules, rulesToText } from '../../data/locke-rules';
import { PokemonApi } from '../../data/pokemon-api';
import { LockeSummary } from '../../data/pokemon-models';
import { ModalOverlay } from '@shared/directives/modal-overlay';

type Dialog = { mode: 'create' } | { mode: 'edit'; id: string } | { mode: 'delete'; locke: LockeSummary } | null;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-admin-lockes',
    imports: [ModalOverlay, FormsModule, RouterLink],
    templateUrl: './admin-lockes.html',
})
export class AdminLockes {
    private readonly api = inject(PokemonApi);
    private readonly notifications = inject(NotificationCenter);
    private readonly router = inject(Router);

    protected readonly lockes = signal<LockeSummary[]>([]);
    protected readonly loading = signal(true);
    protected readonly saving = signal(false);
    protected readonly dialog = signal<Dialog>(null);

    protected form = { name: '', description: '', rules: '' };

    constructor() {
        this.load();
    }

    private load(): void {
        this.api.getLockes().subscribe({
            next: (lockes) => {
                this.lockes.set(lockes);
                this.loading.set(false);
            },
            error: (error) => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se han podido cargar los randomlockes'));
            },
        });
    }

    protected gameNames(locke: LockeSummary): string {
        return locke.games.map((game) => game.name).join(', ') || '—';
    }

    protected openCreate(): void {
        this.form = { name: '', description: '', rules: '' };
        this.dialog.set({ mode: 'create' });
    }

    protected openEdit(summary: LockeSummary): void {
        this.api.getLocke(summary.id).subscribe({
            next: (locke) => {
                this.form = { name: locke.name, description: locke.description ?? '', rules: rulesToText(locke.rules) };
                this.dialog.set({ mode: 'edit', id: locke.id });
            },
            error: (error) => this.notifications.error(resolveApiError(error, 'No se ha podido cargar el randomlocke')),
        });
    }

    protected openDelete(locke: LockeSummary): void {
        this.dialog.set({ mode: 'delete', locke });
    }

    protected close(): void {
        this.dialog.set(null);
    }

    protected save(): void {
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
            next: (locke) => {
                this.saving.set(false);
                this.dialog.set(null);
                this.notifications.success(dialog?.mode === 'edit' ? 'Randomlocke actualizado' : 'Randomlocke creado');

                if (dialog?.mode === 'create') {
                    this.router.navigate(['/admin/pokemon/lockes', locke.id]);
                } else {
                    this.load();
                }
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar'));
            },
        });
    }

    protected confirmDelete(): void {
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
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido eliminar'));
            },
        });
    }
}
