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
import { NotificationService } from '@core/services/notification';
import { resolveApiError } from '@shared/utils/api-error';
import { PokemonApi } from '../../data/pokemon-api';
import { FORMAT_LABELS, FORMAT_OPTIONS, STATUS_LABELS } from '../../data/pokemon-labels';
import { Game, LockeDetail, Player, TournamentCreateRequest } from '../../data/pokemon-models';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-admin-locke',
    imports: [FormsModule, RouterLink],
    templateUrl: './admin-locke.html'
})
export class AdminLocke {

    private readonly api = inject(PokemonApi);
    private readonly notifications = inject(NotificationService);
    private readonly router = inject(Router);

    readonly formatLabels = FORMAT_LABELS;
    readonly formatOptions = FORMAT_OPTIONS;
    readonly statusLabels = STATUS_LABELS;

    readonly loading = signal(true);
    readonly saving = signal(false);
    readonly locke = signal<LockeDetail | null>(null);
    readonly players = signal<Player[]>([]);
    readonly games = signal<Game[]>([]);
    readonly roster = signal<Set<string>>(new Set());
    readonly creating = signal(false);

    form: TournamentCreateRequest = this.emptyForm('');
    revealAt = '';

    constructor() {
        inject(ActivatedRoute).paramMap
            .pipe(switchMap(params => forkJoin({
                locke: this.api.getLocke(params.get('lockeId') ?? ''),
                players: this.api.getPlayers(),
                games: this.api.getGames()
            })))
            .subscribe({
                next: ({ locke, players, games }) => {
                    this.setLocke(locke);
                    this.players.set(players);
                    this.games.set(games);
                    this.loading.set(false);
                },
                error: error => {
                    this.loading.set(false);
                    this.notifications.error(resolveApiError(error, 'No se ha podido cargar el randomlocke'));
                }
            });
    }

    toggleRoster(playerId: string): void {
        this.roster.update(current => {
            const next = new Set(current);

            if (!next.delete(playerId)) {
                next.add(playerId);
            }

            return next;
        });
    }

    saveRoster(): void {
        const locke = this.locke();

        if (!locke) {
            return;
        }

        this.saving.set(true);
        this.api.setLockeRoster(locke.id, [...this.roster()]).subscribe({
            next: updated => {
                this.saving.set(false);
                this.setLocke(updated);
                this.notifications.success('Plantilla guardada');
            },
            error: error => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar la plantilla'));
            }
        });
    }

    openCreate(): void {
        const lastGame = this.locke()?.tournaments.at(-1)?.game.id ?? this.games()[0]?.id ?? '';
        this.form = this.emptyForm(lastGame);
        this.revealAt = '';
        this.creating.set(true);
    }

    createTournament(): void {
        const locke = this.locke();

        if (!locke) {
            return;
        }

        const request: TournamentCreateRequest = {
            ...this.form,
            lockeId: locke.id,
            description: this.form.description?.trim() || null,
            teamsRevealAt: this.revealAt ? new Date(this.revealAt).toISOString() : null
        };

        this.saving.set(true);
        this.api.createTournament(request).subscribe({
            next: tournament => {
                this.saving.set(false);
                this.creating.set(false);
                this.notifications.success('Torneo creado');
                this.router.navigate(['/admin/pokemon/tournaments', tournament.id]);
            },
            error: error => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido crear el torneo'));
            }
        });
    }

    private setLocke(locke: LockeDetail): void {
        this.locke.set(locke);
        this.roster.set(new Set(locke.roster.map(player => player.id)));
    }

    private emptyForm(gameId: string): TournamentCreateRequest {
        return { lockeId: '', gameId, name: '', description: '', format: 'groups_bracket', pointsPerWin: 1, teamsRevealAt: null };
    }
}
