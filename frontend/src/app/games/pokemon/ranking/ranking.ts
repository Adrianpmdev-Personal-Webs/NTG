/**
 * RankingPage
 *
 * All-time ranking of every randomlocke added together. Each randomlocke keeps its own standings on its
 * page; this one is the general record.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { PokemonApi } from '../data/pokemon-api';
import { PlayerStanding } from '../data/pokemon-models';
import { playerRows } from '../data/tournament-view';
import { StandingsTable } from '../ui/standings-table/standings-table';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-ranking',
    imports: [StandingsTable],
    templateUrl: './ranking.html',
    styleUrl: './ranking.scss',
})
export class RankingPage {
    private readonly api = inject(PokemonApi);

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly ranking = signal<PlayerStanding[]>([]);

    protected readonly rows = computed(() => playerRows(this.ranking(), (row) => `${row.titles} / ${row.tournaments}`));

    constructor() {
        this.api.getRanking().subscribe({
            next: (ranking) => {
                this.ranking.set(ranking);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }
}
