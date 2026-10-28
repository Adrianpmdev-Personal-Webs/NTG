/**
 * AdminLocke
 *
 * Management of one randomlocke: its fixed roster (new tournaments start with it) and its tournaments
 * (create one, or open one to manage it).
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, switchMap } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { resolveApiError } from '@shared/utils/api-error';
import { PokemonApi } from '../../data/pokemon-api';
import { FORMAT_LABELS, FORMAT_OPTIONS, STATUS_LABELS } from '../../data/pokemon-labels';
import { Game, LockeDetail, Player, TournamentCreateRequest } from '../../data/pokemon-models';
import { ModalOverlay } from '@shared/directives/modal-overlay';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-admin-locke',
    imports: [ModalOverlay, FormsModule, RouterLink],
    templateUrl: './admin-locke.html',
})
export class AdminLocke {
    private readonly api = inject(PokemonApi);
    private readonly notifications = inject(NotificationCenter);
    private readonly router = inject(Router);

    protected readonly formatLabels = FORMAT_LABELS;
    protected readonly formatOptions = FORMAT_OPTIONS;
    protected readonly statusLabels = STATUS_LABELS;

    protected readonly loading = signal(true);
    protected readonly saving = signal(false);
    protected readonly locke = signal<LockeDetail | null>(null);
    protected readonly players = signal<Player[]>([]);
    protected readonly games = signal<Game[]>([]);
    protected readonly roster = signal<Set<string>>(new Set());
    protected readonly creating = signal(false);

    protected form: TournamentCreateRequest = this.emptyForm('');
    protected revealAt = '';

    constructor() {
        inject(ActivatedRoute)
            .paramMap.pipe(
                switchMap((params) =>
                    forkJoin({
                        locke: this.api.getLocke(params.get('lockeId') ?? ''),
                        players: this.api.getPlayers(),
                        games: this.api.getGames(),
                    }),
                ),
            )
            .subscribe({
                next: ({ locke, players, games }) => {
                    this.setLocke(locke);
                    this.players.set(players);
                    this.games.set(games);
                    this.loading.set(false);
                },
                error: (error) => {
                    this.loading.set(false);
                    this.notifications.error(resolveApiError(error, 'No se ha podido cargar el randomlocke'));
                },
            });
    }

    protected toggleRoster(playerId: string): void {
        this.roster.update((current) => {
            const next = new Set(current);

            if (!next.delete(playerId)) {
                next.add(playerId);
            }

            return next;
        });
    }

    protected saveRoster(): void {
        const locke = this.locke();

        if (!locke) {
            return;
        }

        this.saving.set(true);
        this.api.setLockeRoster(locke.id, [...this.roster()]).subscribe({
            next: (updated) => {
                this.saving.set(false);
                this.setLocke(updated);
                this.notifications.success('Plantilla guardada');
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar la plantilla'));
            },
        });
    }

    protected openCreate(): void {
        const lastGame = this.locke()?.tournaments.at(-1)?.game.id ?? this.games()[0]?.id ?? '';
        this.form = this.emptyForm(lastGame);
        this.revealAt = '';
        this.creating.set(true);
    }

    protected createTournament(): void {
        const locke = this.locke();

        if (!locke) {
            return;
        }

        const request: TournamentCreateRequest = {
            ...this.form,
            lockeId: locke.id,
            description: this.form.description?.trim() || null,
            teamsRevealAt: this.revealAt ? new Date(this.revealAt).toISOString() : null,
        };

        this.saving.set(true);
        this.api.createTournament(request).subscribe({
            next: (tournament) => {
                this.saving.set(false);
                this.creating.set(false);
                this.notifications.success('Torneo creado');
                this.router.navigate(['/admin/pokemon/tournaments', tournament.id]);
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido crear el torneo'));
            },
        });
    }

    private setLocke(locke: LockeDetail): void {
        this.locke.set(locke);
        this.roster.set(new Set(locke.roster.map((player) => player.id)));
    }

    private emptyForm(gameId: string): TournamentCreateRequest {
        return {
            lockeId: '',
            gameId,
            name: '',
            description: '',
            format: 'groups_bracket',
            pointsPerWin: 1,
            teamsRevealAt: null,
        };
    }
}
