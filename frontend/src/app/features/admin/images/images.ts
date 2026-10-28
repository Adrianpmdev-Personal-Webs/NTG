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
import { ACCEPTED_IMAGE_TYPES, AssetApi, checkImageFile } from '@core/assets/asset-api';
import { GameCatalog } from '@core/games/game-catalog';
import { NotificationCenter } from '@core/notifications/notification-center';
import { Asset } from '@shared/models/asset';
import { resolveApiError } from '@shared/utils/api-error';
import { ModalOverlay } from '@shared/directives/modal-overlay';

interface AssetGroup {
    name: string;
    assets: Asset[];
    uploaded: number;
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-admin-images',
    imports: [ModalOverlay, DatePipe],
    templateUrl: './images.html',
    styleUrl: './images.scss',
})
export class Images {
    private readonly assetApi = inject(AssetApi);
    private readonly catalog = inject(GameCatalog);
    private readonly notifications = inject(NotificationCenter);

    protected readonly accepted = ACCEPTED_IMAGE_TYPES;
    protected readonly assets = signal<Asset[]>([]);
    protected readonly loading = signal(true);
    protected readonly busyKey = signal<string | null>(null);
    protected readonly confirmDelete = signal<Asset | null>(null);

    protected readonly groups = computed<AssetGroup[]>(() => {
        const groups = new Map<string, Asset[]>();

        for (const asset of this.assets()) {
            groups.set(asset.group, [...(groups.get(asset.group) ?? []), asset]);
        }

        return [...groups.entries()].map(([name, assets]) => ({
            name,
            assets,
            uploaded: assets.filter((asset) => asset.url).length,
        }));
    });

    constructor() {
        this.load();
    }

    private load(): void {
        this.assetApi.getAll().subscribe({
            next: (assets) => {
                this.assets.set(assets);
                this.loading.set(false);
            },
            error: (error) => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se han podido cargar las imágenes'));
            },
        });
    }

    protected upload(asset: Asset, event: Event): void {
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
        this.assetApi.upload(asset.key, file).subscribe({
            next: (stored) => {
                this.busyKey.set(null);
                this.assets.update((assets) => assets.map((item) => (item.key === stored.key ? stored : item)));
                this.notifications.success(`${asset.label}: imagen guardada (${this.size(stored.sizeBytes)})`);
                this.refreshCatalogIfCover(asset.key);
            },
            error: (error) => {
                this.busyKey.set(null);
                this.notifications.error(resolveApiError(error, 'No se ha podido subir la imagen'));
            },
        });
    }

    protected askDelete(asset: Asset): void {
        this.confirmDelete.set(asset);
    }

    protected closeDelete(): void {
        this.confirmDelete.set(null);
    }

    protected delete(): void {
        const asset = this.confirmDelete();

        if (!asset) {
            return;
        }

        this.busyKey.set(asset.key);
        this.assetApi.delete(asset.key).subscribe({
            next: () => {
                this.busyKey.set(null);
                this.confirmDelete.set(null);
                this.notifications.success(`${asset.label}: imagen eliminada`);
                this.refreshCatalogIfCover(asset.key);
                this.load();
            },
            error: (error) => {
                this.busyKey.set(null);
                this.notifications.error(resolveApiError(error, 'No se ha podido eliminar la imagen'));
            },
        });
    }

    protected size(bytes: number | null): string {
        if (bytes === null) {
            return '—';
        }

        return bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
    }

    private refreshCatalogIfCover(key: string): void {
        if (key.startsWith('platform/games/')) {
            this.catalog.refresh();
        }
    }
}
