/**
 * BaseData
 *
 * Admin screen for the WoW Forever base data: classes, races (with the classes each one can play) and settings.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { NotificationService } from '@core/services/notification';
import { FACTION_LABELS, hideBrokenIcon, wowIconUrl } from '../../wow-forever-config';
import { WowForeverApi } from '../../wow-forever-api';
import {
    Faction,
    Race,
    RaceCreateRequest,
    WowClass,
    WowClassCreateRequest,
    WowSetting
} from '../../wow-forever-models';

type Tab = 'classes' | 'races' | 'settings';
type Kind = 'class' | 'race';

interface ModalState {
    kind: Kind;
    /** Id of the item being edited, or null when creating. */
    editingId: string | null;
}

interface DeleteTarget {
    kind: Kind;
    id: string;
    name: string;
}

const emptyClassForm = (): WowClassCreateRequest => ({ slug: '', name: '', icon: '', sortOrder: 0 });

const emptyRaceForm = (): RaceCreateRequest => ({
    slug: '',
    name: '',
    faction: 'ALLIANCE',
    icon: '',
    sortOrder: 0,
    classIds: []
});

@Component({
    selector: 'app-wow-forever-base-data',
    imports: [FormsModule],
    templateUrl: './base-data.html',
    styleUrl: './base-data.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BaseData implements OnInit {

    private readonly api = inject(WowForeverApi);
    private readonly notificationService = inject(NotificationService);

    readonly tabs: readonly { id: Tab; label: string }[] = [
        { id: 'classes', label: 'CLASES' },
        { id: 'races', label: 'RAZAS' },
        { id: 'settings', label: 'AJUSTES' }
    ];
    readonly factions: readonly Faction[] = ['ALLIANCE', 'HORDE'];
    readonly factionLabels = FACTION_LABELS;

    readonly tab = signal<Tab>('classes');
    readonly classes = signal<WowClass[]>([]);
    readonly races = signal<Race[]>([]);
    readonly settings = signal<WowSetting[]>([]);
    readonly loading = signal(true);
    readonly submitting = signal(false);
    readonly modal = signal<ModalState | null>(null);
    readonly deleteTarget = signal<DeleteTarget | null>(null);

    classForm: WowClassCreateRequest = emptyClassForm();
    raceForm: RaceCreateRequest = emptyRaceForm();
    settingDrafts: Record<string, string> = {};

    readonly iconUrl = wowIconUrl;
    readonly onIconError = hideBrokenIcon;

    ngOnInit(): void {
        this.load();
    }

    load(): void {
        this.loading.set(true);

        forkJoin({
            classes: this.api.getClasses(),
            races: this.api.getRaces(),
            settings: this.api.getSettings()
        }).subscribe({
            next: ({ classes, races, settings }) => {
                this.classes.set(classes);
                this.races.set(races);
                this.settings.set(settings);
                this.settingDrafts = Object.fromEntries(settings.map(setting => [setting.key, setting.value]));
                this.loading.set(false);
            },
            error: error => {
                this.notificationService.error(this.resolveError(error, 'Error al cargar los datos'));
                this.loading.set(false);
            }
        });
    }

    className(id: string): string {
        return this.classes().find(wowClass => wowClass.id === id)?.name ?? '?';
    }

    isEditing(): boolean {
        return this.modal()?.editingId != null;
    }

    // ─── Classes ───

    openCreateClass(): void {
        this.classForm = emptyClassForm();
        this.classForm.sortOrder = this.classes().length + 1;
        this.modal.set({ kind: 'class', editingId: null });
    }

    openEditClass(wowClass: WowClass): void {
        this.classForm = {
            slug: wowClass.slug,
            name: wowClass.name,
            icon: wowClass.icon,
            sortOrder: wowClass.sortOrder
        };
        this.modal.set({ kind: 'class', editingId: wowClass.id });
    }

    submitClass(): void {
        const form = this.classForm;

        if (!form.name.trim() || !form.icon.trim() || (!this.isEditing() && !form.slug.trim())) {
            this.notificationService.warning('Completa todos los campos');
            return;
        }

        const editingId = this.modal()?.editingId ?? null;
        const request$ = editingId
            ? this.api.updateClass(editingId, { name: form.name, icon: form.icon, sortOrder: form.sortOrder })
            : this.api.createClass({ ...form, slug: form.slug.trim() });

        this.save(request$, editingId ? 'Clase actualizada' : 'Clase creada');
    }

    // ─── Races ───

    openCreateRace(): void {
        this.raceForm = emptyRaceForm();
        this.raceForm.sortOrder = this.races().length + 1;
        this.modal.set({ kind: 'race', editingId: null });
    }

    openEditRace(race: Race): void {
        this.raceForm = {
            slug: race.slug,
            name: race.name,
            faction: race.faction,
            icon: race.icon,
            sortOrder: race.sortOrder,
            classIds: [...race.classIds]
        };
        this.modal.set({ kind: 'race', editingId: race.id });
    }

    toggleRaceClass(classId: string): void {
        const ids = this.raceForm.classIds;
        this.raceForm.classIds = ids.includes(classId) ? ids.filter(id => id !== classId) : [...ids, classId];
    }

    submitRace(): void {
        const form = this.raceForm;

        if (!form.name.trim() || !form.icon.trim() || (!this.isEditing() && !form.slug.trim())) {
            this.notificationService.warning('Completa todos los campos');
            return;
        }

        const editingId = this.modal()?.editingId ?? null;
        const request$ = editingId
            ? this.api.updateRace(editingId, {
                name: form.name,
                faction: form.faction,
                icon: form.icon,
                sortOrder: form.sortOrder,
                classIds: form.classIds
            })
            : this.api.createRace({ ...form, slug: form.slug.trim() });

        this.save(request$, editingId ? 'Raza actualizada' : 'Raza creada');
    }

    // ─── Settings ───

    saveSetting(setting: WowSetting): void {
        const value = (this.settingDrafts[setting.key] ?? '').trim();

        if (!value) {
            this.notificationService.warning('El valor no puede estar vacío');
            return;
        }

        this.api.updateSetting(setting.key, value).subscribe({
            next: () => {
                this.notificationService.success('Ajuste guardado');
                this.load();
            },
            error: error => this.notificationService.error(this.resolveError(error, 'Error al guardar el ajuste'))
        });
    }

    // ─── Shared ───

    closeModal(): void {
        this.modal.set(null);
    }

    askDelete(kind: Kind, id: string, name: string): void {
        this.deleteTarget.set({ kind, id, name });
    }

    cancelDelete(): void {
        this.deleteTarget.set(null);
    }

    confirmDelete(): void {
        const target = this.deleteTarget();

        if (!target) {
            return;
        }

        const request$ = target.kind === 'class' ? this.api.deleteClass(target.id) : this.api.deleteRace(target.id);

        this.submitting.set(true);
        request$.subscribe({
            next: () => {
                this.notificationService.success(`${target.name} eliminado`);
                this.deleteTarget.set(null);
                this.submitting.set(false);
                this.load();
            },
            error: error => {
                this.notificationService.error(this.resolveError(error, 'No se ha podido eliminar'));
                this.submitting.set(false);
            }
        });
    }

    private save(request$: ReturnType<WowForeverApi['createClass']> | ReturnType<WowForeverApi['createRace']>, message: string): void {
        this.submitting.set(true);

        request$.subscribe({
            next: () => {
                this.notificationService.success(message);
                this.closeModal();
                this.submitting.set(false);
                this.load();
            },
            error: error => {
                this.notificationService.error(this.resolveError(error, 'Error al guardar'));
                this.submitting.set(false);
            }
        });
    }

    private resolveError(error: unknown, fallback: string): string {
        const detail = (error as { error?: { detail?: string } })?.error?.detail;

        return detail || fallback;
    }
}
