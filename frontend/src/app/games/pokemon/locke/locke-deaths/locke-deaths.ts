/**
 * LockeDeathsView
 *
 * The Pokémon lost in a randomlocke: one card per player with the names and their total, and the total of the
 * randomlocke. Whoever the backend allows can add a name or remove one; the backend writes the names the same way
 * and answers with the updated list. It is only asked for when its tab is opened.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { NotificationCenter } from '@core/notifications/notification-center';
import { resolveApiError } from '@shared/utils/api-error';
import { PokemonApi } from '../../data/pokemon-api';
import { LockeDeath, LockeDeaths } from '../../data/pokemon-models';
import { NavIcon } from '@shared/components/nav-icon/nav-icon';

const POKEMON_NAME_MAX_LENGTH = 30;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-locke-deaths',
    imports: [NavIcon],
    templateUrl: './locke-deaths.html',
    styleUrl: './locke-deaths.scss',
})
export class LockeDeathsView implements OnInit {
    private readonly api = inject(PokemonApi);
    private readonly notifications = inject(NotificationCenter);

    readonly lockeId = input.required<string>();

    protected readonly nameMaxLength = POKEMON_NAME_MAX_LENGTH;
    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly saving = signal(false);
    protected readonly deaths = signal<LockeDeaths | null>(null);

    ngOnInit(): void {
        this.api.getLockeDeaths(this.lockeId()).subscribe({
            next: (deaths) => {
                this.deaths.set(deaths);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected add(event: Event, playerId: string, field: HTMLInputElement): void {
        event.preventDefault();

        const pokemon = field.value.trim();

        if (!pokemon || this.saving()) {
            return;
        }

        this.saving.set(true);
        this.api.addLockeDeath(this.lockeId(), playerId, pokemon).subscribe({
            next: (deaths) => {
                this.deaths.set(deaths);
                this.saving.set(false);
                field.value = '';
                field.focus();
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(
                    error.status === 400
                        ? 'Ese nombre no es válido: usa solo letras y números'
                        : resolveApiError(error, 'No se ha podido apuntar el Pokémon'),
                );
            },
        });
    }

    protected remove(death: LockeDeath): void {
        if (this.saving()) {
            return;
        }

        this.saving.set(true);
        this.api.deleteLockeDeath(this.lockeId(), death.id).subscribe({
            next: (deaths) => {
                this.deaths.set(deaths);
                this.saving.set(false);
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido quitar el Pokémon'));
            },
        });
    }
}
