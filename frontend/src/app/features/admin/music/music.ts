/**
 * Music
 *
 * Admin screen of the background music: one playlist for the general web and one per game, in the order the backend
 * gives them. The admin adds songs in any format the browser can read; each one is compressed here (see compressSong)
 * and only the small M4A is uploaded. Songs can be listened to, renamed and deleted. After every
 * change the player of this browser reads the playlists again.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MusicApi } from '@core/music/music-api';
import { MusicPlayer } from '@core/music/music-player';
import { NotificationCenter } from '@core/notifications/notification-center';
import { MusicArea, MusicTrack } from '@shared/models/music';
import { resolveApiError } from '@shared/utils/api-error';
import { ModalOverlay } from '@shared/directives/modal-overlay';
import { SongError, compressSong } from './song-compressor';
import { NavIcon } from '@shared/components/nav-icon/nav-icon';

const MAX_TITLE_LENGTH = 100;

interface Progress {
    area: string;
    text: string;
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-admin-music',
    imports: [NavIcon, ModalOverlay],
    templateUrl: './music.html',
    styleUrl: './music.scss',
})
export class Music {
    private readonly musicApi = inject(MusicApi);
    private readonly player = inject(MusicPlayer);
    private readonly notifications = inject(NotificationCenter);

    protected readonly areas = signal<MusicArea[]>([]);
    protected readonly loading = signal(true);
    protected readonly busy = signal(false);
    protected readonly progress = signal<Progress | null>(null);
    protected readonly confirmDelete = signal<MusicTrack | null>(null);

    constructor() {
        this.load();
    }

    protected async add(area: MusicArea, event: Event): Promise<void> {
        const input = event.target as HTMLInputElement;
        const files = [...(input.files ?? [])];
        input.value = '';

        if (files.length === 0) {
            return;
        }

        this.busy.set(true);
        let added = 0;

        for (const [index, file] of files.entries()) {
            const step = files.length > 1 ? `${index + 1}/${files.length} · ` : '';
            const title = this.titleOf(file);

            try {
                this.progress.set({ area: area.area, text: `${step}COMPRIMIENDO ${title}` });
                const song = await compressSong(file, (value) =>
                    this.progress.set({
                        area: area.area,
                        text: `${step}COMPRIMIENDO ${title} · ${Math.round(value * 100)}%`,
                    }),
                );

                this.progress.set({ area: area.area, text: `${step}SUBIENDO ${title} · ${this.size(song.blob.size)}` });
                await firstValueFrom(this.musicApi.upload(area.area, song.blob, title, song.durationSeconds));
                added++;
            } catch (error) {
                const reason =
                    error instanceof SongError ? error.message : resolveApiError(error, 'No se ha podido subir');
                this.notifications.error(`${file.name}: ${reason}`);
            }
        }

        this.progress.set(null);
        this.busy.set(false);

        if (added > 0) {
            this.notifications.success(
                `${area.name}: ${added} ${added === 1 ? 'canción añadida' : 'canciones añadidas'}`,
            );
            this.changed();
        }
    }

    protected rename(track: MusicTrack, event: Event): void {
        const input = event.target as HTMLInputElement;
        const title = input.value.trim().slice(0, MAX_TITLE_LENGTH);

        if (!title || title === track.title) {
            input.value = track.title;
            return;
        }

        this.busy.set(true);
        this.musicApi.rename(track.id, title).subscribe({
            next: () => {
                this.busy.set(false);
                this.notifications.success(`Canción renombrada: ${title}`);
                this.changed();
            },
            error: (error) => {
                this.busy.set(false);
                input.value = track.title;
                this.notifications.error(resolveApiError(error, 'No se ha podido renombrar la canción'));
            },
        });
    }

    protected askDelete(track: MusicTrack): void {
        this.confirmDelete.set(track);
    }

    protected closeDelete(): void {
        this.confirmDelete.set(null);
    }

    protected delete(): void {
        const track = this.confirmDelete();

        if (!track) {
            return;
        }

        this.busy.set(true);
        this.musicApi.delete(track.id).subscribe({
            next: () => {
                this.busy.set(false);
                this.confirmDelete.set(null);
                this.notifications.success(`Canción eliminada: ${track.title}`);
                this.changed();
            },
            error: (error) => {
                this.busy.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido eliminar la canción'));
            },
        });
    }

    protected duration(seconds: number): string {
        return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
    }

    protected size(bytes: number): string {
        return bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
    }

    private load(): void {
        this.musicApi.getAreas().subscribe({
            next: (areas) => {
                this.areas.set(areas);
                this.loading.set(false);
            },
            error: (error) => {
                this.loading.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido cargar la música'));
            },
        });
    }

    private changed(): void {
        this.load();
        this.player.reload();
    }

    private titleOf(file: File): string {
        const title = file.name
            .replace(/\.[^.]+$/, '')
            .replace(/[_]+/g, ' ')
            .trim();

        return (title || 'Canción').slice(0, MAX_TITLE_LENGTH);
    }
}
