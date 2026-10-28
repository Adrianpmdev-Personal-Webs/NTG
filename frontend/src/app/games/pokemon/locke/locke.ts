/**
 * LockePage
 *
 * One randomlocke: its tournaments in play order (with their game and champion), its cumulative
 * standings, its rules and its players.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { PokemonApi } from '../data/pokemon-api';
import { FORMAT_LABELS, STATUS_LABELS } from '../data/pokemon-labels';
import { LockeDetail } from '../data/pokemon-models';
import { playerRows } from '../data/tournament-view';
import { RulesView } from '../ui/rules-view/rules-view';
import { StandingsTable } from '../ui/standings-table/standings-table';

type LockeTab = 'tournaments' | 'standings' | 'rules' | 'players';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-locke',
    imports: [RouterLink, StandingsTable, RulesView],
    templateUrl: './locke.html',
    styleUrl: './locke.scss',
})
export class LockePage {
    private readonly api = inject(PokemonApi);

    protected readonly tabs: readonly { id: LockeTab; label: string }[] = [
        { id: 'tournaments', label: 'Torneos' },
        { id: 'standings', label: 'Clasificación' },
        { id: 'rules', label: 'Reglas' },
        { id: 'players', label: 'Jugadores' },
    ];

    protected readonly formatLabels = FORMAT_LABELS;
    protected readonly statusLabels = STATUS_LABELS;

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly locke = signal<LockeDetail | null>(null);
    protected readonly activeTab = signal<LockeTab>('tournaments');

    protected readonly standingRows = computed(() =>
        playerRows(this.locke()?.standings ?? [], (row) => `${row.titles}`),
    );

    constructor() {
        inject(ActivatedRoute)
            .paramMap.pipe(
                switchMap((params) => {
                    this.loading.set(true);
                    return this.api.getLocke(params.get('lockeId') ?? '');
                }),
            )
            .subscribe({
                next: (locke) => {
                    this.locke.set(locke);
                    this.failed.set(false);
                    this.loading.set(false);
                },
                error: () => {
                    this.failed.set(true);
                    this.loading.set(false);
                },
            });
    }
}
