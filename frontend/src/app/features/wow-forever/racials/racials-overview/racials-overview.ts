/**
 * RacialsOverview
 *
 * Races of WoW Forever: the racial abilities of each race (and the class abilities that change with the race), plus a
 * table of which race can play which class. The races can be narrowed down to one faction.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { GameDataApi } from '../../game-data/game-data-api';
import { GameClass } from '../../game-data/game-data-models';
import { WowPage } from '../../ui/wow-page/wow-page';
import { FACTION_LABELS, creditLine, hideBrokenIcon, wowIconUrl } from '../../wow-forever-config';
import { Faction } from '../../wow-forever-models';
import { Race, RacialsData } from '../racials-models';

type FactionFilter = Faction | 'ALL';

@Component({
    selector: 'app-racials-overview',
    imports: [WowPage],
    templateUrl: './racials-overview.html',
    styleUrl: './racials-overview.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class RacialsOverview implements OnInit {

    private readonly gameData = inject(GameDataApi);

    readonly filters: readonly { id: FactionFilter; label: string }[] = [
        { id: 'ALL', label: 'Todas' },
        { id: 'ALLIANCE', label: FACTION_LABELS.ALLIANCE },
        { id: 'HORDE', label: FACTION_LABELS.HORDE }
    ];
    readonly factionLabels = FACTION_LABELS;

    readonly data = signal<RacialsData | null>(null);
    readonly classes = signal<GameClass[]>([]);
    readonly loading = signal(true);
    readonly failed = signal(false);
    readonly filter = signal<FactionFilter>('ALL');

    readonly races = computed<Race[]>(() => {
        const filter = this.filter();
        const races = this.data()?.races ?? [];

        return filter === 'ALL' ? races : races.filter(race => race.faction === filter);
    });

    readonly credit = computed(() => creditLine(this.data()?.credit ?? null));

    readonly iconUrl = wowIconUrl;
    readonly onIconError = hideBrokenIcon;

    ngOnInit(): void {
        forkJoin({ index: this.gameData.getIndex(), racials: this.gameData.getRacials() }).subscribe({
            next: ({ index, racials }) => {
                this.classes.set(index.classes);
                this.data.set(racials);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            }
        });
    }

    setFilter(filter: FactionFilter): void {
        this.filter.set(filter);
    }

    canPlay(race: Race, gameClass: GameClass): boolean {
        return race.classNames.includes(gameClass.name);
    }

    /** Classes of the race in the order of the class list, with their icons. */
    classesOf(race: Race): GameClass[] {
        return this.classes().filter(gameClass => this.canPlay(race, gameClass));
    }
}
