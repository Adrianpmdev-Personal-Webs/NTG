/**
 * Images
 *
 * Admin screen of the public pictures (game covers, WoW backgrounds and talent trees...). The list comes from the
 * backend: every picture the platform and the games expect, grouped, with its preview. The admin uploads the original
 * picture and the backend compresses it.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ACCEPTED_IMAGE_TYPES, AssetService, checkImageFile } from '@core/services/asset';
import { GameCatalogService } from '@core/services/game-catalog';
import { NotificationService } from '@core/services/notification';
import { Asset } from '@shared/models/asset';
import { resolveApiError } from '@shared/utils/api-error';

interface AssetGroup {
    name: string;
    assets: Asset[];
    uploaded: number;
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-admin-images',
    imports: [DatePipe],
    templateUrl: './images.html',
    styleUrl: './images.scss'
})
export class Images {

    private readonly assetService = inject(AssetService);
    private readonly catalog = inject(GameCatalogService);
    private readonly notifications = inject(NotificationService);

    readonly accepted = ACCEPTED_IMAGE_TYPES;
    readonly assets = signal<Asset[]>([]);
    readonly loading = signal(true);
    /** Key of the picture being uploaded or deleted. */
    readonly busyKey = signal<string | null>(null);
    readonly confirmDelete = signal<Asset | null>(null);

    /** Pictures grouped as the backend orders them. */
    readonly groups = computed<AssetGroup[]>(() => {
        const groups = new Map<string, Asset[]>();

        for (const asset of this.assets()) {
            groups.set(asset.group, [...(groups.get(asset.group) ?? []), asset]);
        }

        return [...groups.entries()].map(([name, assets]) => ({
            name,
            assets,
            uploaded: assets.filter(asset => asset.url).length
        }));
    });

    constructor() {
        this.load();
    }

    load(): void {
        this.assetService.getAll().subscribe({
            next: assets => {
                this.assets.set(assets);
                this.loading.set(false);
            },
            error: error => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se han podido cargar las imágenes'));
            }
        });
    }

    upload(asset: Asset, event: Event): void {
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

        this.busyKey.set(asset.key);
        this.assetService.upload(asset.key, file).subscribe({
            next: stored => {
                this.busyKey.set(null);
                this.assets.update(assets => assets.map(item => item.key === stored.key ? stored : item));
                this.notifications.success(`${asset.label}: imagen guardada (${this.size(stored.sizeBytes)})`);
                this.refreshCatalogIfCover(asset.key);
            },
            error: error => {
                this.busyKey.set(null);
                this.notifications.error(resolveApiError(error, 'No se ha podido subir la imagen'));
            }
        });
    }

    askDelete(asset: Asset): void {
        this.confirmDelete.set(asset);
    }

    closeDelete(): void {
        this.confirmDelete.set(null);
    }

    delete(): void {
        const asset = this.confirmDelete();

        if (!asset) {
            return;
        }

        this.busyKey.set(asset.key);
        this.assetService.delete(asset.key).subscribe({
            next: () => {
                this.busyKey.set(null);
                this.confirmDelete.set(null);
                this.notifications.success(`${asset.label}: imagen eliminada`);
                this.refreshCatalogIfCover(asset.key);
                this.load();
            },
            error: error => {
                this.busyKey.set(null);
                this.notifications.error(resolveApiError(error, 'No se ha podido eliminar la imagen'));
            }
        });
    }

    /** "245 KB", "1.2 MB" or "—". */
    size(bytes: number | null): string {
        if (bytes === null) {
            return '—';
        }

        return bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
    }

    /** Covers are shown by the sidebar, the welcome page and the dashboard through the catalog. */
    private refreshCatalogIfCover(key: string): void {
        if (key.startsWith('platform/games/')) {
            this.catalog.refresh();
        }
    }
}
