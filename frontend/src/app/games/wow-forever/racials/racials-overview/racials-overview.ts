/**
 * RacialsOverview
 *
 * Races of WoW Forever: one card per race (faction, playable classes, racial abilities and, folded, the class abilities
 * that change with the race), plus a table of which race can play which class. The description of an ability shows
 * as a tooltip from the pointer, or below the ability when it gets focus. The races can be narrowed down to one
 * faction.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { GameDataApi } from '../../game-data/game-data-api';
import { Ability, GameClass } from '../../game-data/game-data-models';
import { AbilityTooltip } from '../../ui/ability-tooltip/ability-tooltip';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon } from '../../wow-forever-config';
import { Faction } from '../../wow-forever-models';
import { Race, RacialsData } from '../racials-models';

type FactionFilter = Faction | 'ALL';

interface AbilityHover {
    ability: Ability;
    x: number;
    y: number;
}

@Component({
    selector: 'app-racials-overview',
    imports: [AbilityTooltip, WowPage],
    templateUrl: './racials-overview.html',
    styleUrl: './racials-overview.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RacialsOverview implements OnInit {
    private readonly gameData = inject(GameDataApi);

    protected readonly factionLabels = signal<Record<Faction, string>>({ ALLIANCE: '', HORDE: '' });

    protected readonly filters = computed<readonly { id: FactionFilter; label: string }[]>(() => [
        { id: 'ALL', label: 'Todas' },
        { id: 'ALLIANCE', label: this.factionLabels().ALLIANCE },
        { id: 'HORDE', label: this.factionLabels().HORDE },
    ]);

    private readonly data = signal<RacialsData | null>(null);
    protected readonly classes = signal<GameClass[]>([]);
    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly filter = signal<FactionFilter>('ALL');
    protected readonly hovered = signal<AbilityHover | null>(null);

    protected readonly races = computed<Race[]>(() => {
        const filter = this.filter();
        const races = this.data()?.races ?? [];

        return filter === 'ALL' ? races : races.filter((race) => race.faction === filter);
    });

    protected readonly credit = computed(() => creditLine(this.data()?.credit ?? null));

    protected readonly hideBrokenIcon = hideBrokenIcon;

    ngOnInit(): void {
        forkJoin({ index: this.gameData.getIndex(), racials: this.gameData.getRacials() }).subscribe({
            next: ({ index, racials }) => {
                this.classes.set(index.classes);
                this.factionLabels.set(index.factions);
                this.data.set(racials);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected setFilter(filter: FactionFilter): void {
        this.filter.set(filter);
        this.hovered.set(null);
    }

    protected canPlay(race: Race, gameClass: GameClass): boolean {
        return race.classNames.includes(gameClass.name);
    }

    protected classesOf(race: Race): GameClass[] {
        return this.classes().filter((gameClass) => this.canPlay(race, gameClass));
    }

    protected showAbilityAtPointer(event: MouseEvent, ability: Ability): void {
        this.hovered.set({ ability, x: event.clientX, y: event.clientY });
    }

    protected showAbilityBelow(event: FocusEvent, ability: Ability): void {
        const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();

        this.hovered.set({ ability, x: rect.left + rect.width / 2, y: rect.bottom });
    }

    protected hideAbility(): void {
        this.hovered.set(null);
    }
}
