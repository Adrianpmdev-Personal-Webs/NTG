/**
 * ProfessionsOverview
 *
 * Professions of WoW Forever by group (crafting, gathering and secondary): one card per profession with its icon, name
 * and what it is for, its first camp feature (item icon and name, the skill it needs and, in a green box, the buff it
 * gives) and the profession it usually goes with. The description of the camp item shows as a tooltip from the
 * pointer, or below the item when it gets focus. Every text of the screen is in English.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { GameDataApi } from '../../game-data/game-data-api';
import { Ability } from '../../game-data/game-data-models';
import { AbilityTooltip } from '../../ui/ability-tooltip/ability-tooltip';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon } from '../../wow-forever-config';
import { CampFeature, ProfessionsData } from '../professions-models';

interface CampHover {
    item: Ability;
    x: number;
    y: number;
}

@Component({
    selector: 'app-professions-overview',
    imports: [AbilityTooltip, WowPage],
    templateUrl: './professions-overview.html',
    styleUrl: './professions-overview.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfessionsOverview implements OnInit {
    private readonly gameData = inject(GameDataApi);

    private readonly data = signal<ProfessionsData | null>(null);
    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly hovered = signal<CampHover | null>(null);

    protected readonly groups = computed(() => this.data()?.groups ?? []);
    protected readonly credit = computed(() => creditLine(this.data()?.credit ?? null));

    protected readonly hideBrokenIcon = hideBrokenIcon;

    ngOnInit(): void {
        this.gameData.getProfessions().subscribe({
            next: (professions) => {
                this.data.set(professions);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected showCampAtPointer(event: MouseEvent, camp: CampFeature): void {
        this.hovered.set({ item: this.asTooltip(camp), x: event.clientX, y: event.clientY });
    }

    protected showCampBelow(event: FocusEvent, camp: CampFeature): void {
        const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();

        this.hovered.set({ item: this.asTooltip(camp), x: rect.left + rect.width / 2, y: rect.bottom });
    }

    protected hideCamp(): void {
        this.hovered.set(null);
    }

    private asTooltip(camp: CampFeature): Ability {
        return { name: camp.name, text: camp.description, icon: camp.icon, cooldown: null };
    }
}
