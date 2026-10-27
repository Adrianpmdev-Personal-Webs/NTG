/**
 * TeamGallery
 *
 * Team pictures of the players of a tournament. Each card shows the picture when the caller can see it,
 * and an upload button when the backend says the caller may change it (their own team before the
 * reveal, or any team for admins). The original picture is sent: the backend validates and compresses it.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { ACCEPTED_IMAGE_TYPES, checkImageFile } from '@core/services/asset';
import { NotificationService } from '@core/services/notification';
import { resolveApiError } from '@shared/utils/api-error';
import { PokemonApi } from '../../data/pokemon-api';
import { TournamentDetail, TournamentPlayer } from '../../data/pokemon-models';
import { formatDateTime } from '../../data/tournament-view';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-team-gallery',
    templateUrl: './team-gallery.html',
    styleUrl: './team-gallery.scss'
})
export class TeamGallery {

    private readonly api = inject(PokemonApi);
    private readonly notifications = inject(NotificationService);

    readonly detail = input.required<TournamentDetail>();

    /** Emitted after a picture changes, so the parent reloads the tournament. */
    readonly changed = output<void>();

    readonly accepted = ACCEPTED_IMAGE_TYPES;
    readonly busy = signal<string | null>(null);

    revealText(): string {
        return formatDateTime(this.detail().teamsRevealAt);
    }

    onFile(player: TournamentPlayer, event: Event): void {
        const inputElement = event.target as HTMLInputElement;
        const file = inputElement.files?.[0];
        inputElement.value = '';

        if (!file) {
            return;
        }

        const problem = checkImageFile(file);

        if (problem) {
            this.notifications.warning(problem);
            return;
        }

        this.busy.set(player.id);
        this.api.uploadTeamImage(player.id, file).subscribe({
            next: () => {
                this.busy.set(null);
                this.notifications.success('Equipo subido');
                this.changed.emit();
            },
            error: error => {
                this.busy.set(null);
                this.notifications.error(resolveApiError(error, 'No se ha podido subir el equipo'));
            }
        });
    }

    remove(player: TournamentPlayer): void {
        this.busy.set(player.id);
        this.api.deleteTeamImage(player.id).subscribe({
            next: () => {
                this.busy.set(null);
                this.notifications.success('Equipo eliminado');
                this.changed.emit();
            },
            error: error => {
                this.busy.set(null);
                this.notifications.error(resolveApiError(error, 'No se ha podido eliminar el equipo'));
            }
        });
    }
}
