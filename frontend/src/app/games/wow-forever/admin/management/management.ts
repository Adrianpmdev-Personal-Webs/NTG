/**
 * Management
 *
 * Admin management screen of WoW Forever: the module settings. The game data (classes, races, talents, spellbooks,
 * Legacy) is not managed here: it all comes from the data.json file of the backend.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '@core/services/notification';
import { resolveApiError } from '@shared/utils/api-error';
import { WowForeverApi } from '../../wow-forever-api';
import { WowSetting } from '../../wow-forever-models';

@Component({
    selector: 'app-wow-forever-management',
    imports: [FormsModule],
    templateUrl: './management.html',
    styleUrl: './management.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class Management implements OnInit {

    private readonly api = inject(WowForeverApi);
    private readonly notificationService = inject(NotificationService);

    readonly settings = signal<WowSetting[]>([]);
    readonly loading = signal(true);

    settingDrafts: Record<string, string> = {};

    ngOnInit(): void {
        this.load();
    }

    load(): void {
        this.loading.set(true);

        this.api.getSettings().subscribe({
            next: settings => {
                this.settings.set(settings);
                this.settingDrafts = Object.fromEntries(settings.map(setting => [setting.key, setting.value]));
                this.loading.set(false);
            },
            error: error => {
                this.notificationService.error(resolveApiError(error, 'Error al cargar los ajustes'));
                this.loading.set(false);
            }
        });
    }

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
            error: error => this.notificationService.error(resolveApiError(error, 'Error al guardar el ajuste'))
        });
    }
}
