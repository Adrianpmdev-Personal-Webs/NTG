/**
 * SpellbookViewer
 *
 * Spellbook of each WoW Forever class: the spells of every tab with the tooltip of each rank, the level it is learned
 * at and how it compares with Classic. Spells can be filtered by name and by the highest level to show. The spellbook
 * of a class is downloaded when it is picked; the class lives in the URL so the page can be shared.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { GameDataApi } from '../../game-data/game-data-api';
import { ClassicStatus, DataCredit, GameClass } from '../../game-data/game-data-models';
import { ClassPicker } from '../../ui/class-picker/class-picker';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon } from '../../wow-forever-config';
import { ClassSpellbook, Spell, SpellRank } from '../spellbook-models';

interface SpellView {
    spell: Spell;
    ranks: SpellRank[];
    selected: number;
    rank: SpellRank;
}

const MIN_LEVEL = 1;
const MAX_LEVEL = 60;

const CLASSIC_BADGES: Readonly<Partial<Record<ClassicStatus, string>>> = {
    new: 'Nuevo',
    changed: 'Cambiado',
};

const normalize = (text: string): string =>
    text
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .trim();

@Component({
    selector: 'app-spellbook-viewer',
    imports: [ClassPicker, WowPage],
    templateUrl: './spellbook-viewer.html',
    styleUrl: './spellbook-viewer.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpellbookViewer implements OnInit {
    private readonly gameData = inject(GameDataApi);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notificationCenter = inject(NotificationCenter);

    private classLoad: Subscription | null = null;

    protected readonly minLevel = MIN_LEVEL;
    protected readonly maxLevel = MAX_LEVEL;

    protected readonly classes = signal<GameClass[]>([]);
    protected readonly selectedClass = signal<GameClass | null>(null);
    protected readonly spellbook = signal<ClassSpellbook | null>(null);
    private readonly indexCredit = signal<DataCredit | null>(null);
    protected readonly loading = signal(true);
    protected readonly loadingBook = signal(false);
    protected readonly failed = signal(false);
    protected readonly activeTab = signal(0);
    protected readonly query = signal('');
    protected readonly levelLimit = signal(MAX_LEVEL);
    private readonly pickedRanks = signal<Readonly<Record<string, number>>>({});

    protected readonly tabs = computed(() => this.spellbook()?.tabs ?? []);

    protected readonly views = computed<SpellView[]>(() => {
        const tab = this.tabs()[this.activeTab()];
        const query = normalize(this.query());
        const limit = this.levelLimit();
        const picked = this.pickedRanks();

        if (!tab) {
            return [];
        }

        return tab.spells.flatMap((spell) => {
            if (query && !normalize(spell.name).includes(query)) {
                return [];
            }

            const ranks = spell.ranks.filter((rank) => rank.level === null || rank.level <= limit);

            if (ranks.length === 0) {
                return [];
            }

            const chosen = picked[spell.name];
            const selected = chosen !== undefined && chosen < ranks.length ? chosen : ranks.length - 1;

            return [{ spell, ranks, selected, rank: ranks[selected] }];
        });
    });

    protected readonly credit = computed(() => creditLine(this.spellbook()?.credit ?? this.indexCredit()));

    protected readonly hideBrokenIcon = hideBrokenIcon;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.classLoad?.unsubscribe());
    }

    ngOnInit(): void {
        this.gameData.getIndex().subscribe({
            next: (index) => {
                this.classes.set(index.classes);
                this.indexCredit.set(index.credit);
                this.loading.set(false);

                const fromUrl = index.classes.find(
                    (gameClass) => gameClass.slug === this.route.snapshot.queryParamMap.get('class'),
                );
                const first = fromUrl ?? index.classes[0];

                if (first) {
                    this.selectClass(first);
                }
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected selectClass(gameClass: GameClass): void {
        this.selectedClass.set(gameClass);
        this.spellbook.set(null);
        this.activeTab.set(0);
        this.pickedRanks.set({});

        this.classLoad?.unsubscribe();
        this.loadingBook.set(true);
        this.classLoad = this.gameData.getSpellbook(gameClass.slug).subscribe({
            next: (spellbook) => {
                this.spellbook.set(spellbook);
                this.loadingBook.set(false);
            },
            error: () => {
                this.loadingBook.set(false);
                this.notificationCenter.error(`No se ha podido cargar el spellbook de ${gameClass.name}`);
            },
        });

        this.router.navigate([], { relativeTo: this.route, queryParams: { class: gameClass.slug }, replaceUrl: true });
    }

    protected selectTab(index: number): void {
        this.activeTab.set(index);
    }

    protected changeQuery(value: string): void {
        this.query.set(value);
    }

    protected changeLevel(value: number | string): void {
        const parsed = Number(value);

        if (Number.isFinite(parsed)) {
            this.levelLimit.set(Math.min(MAX_LEVEL, Math.max(MIN_LEVEL, Math.round(parsed))));
        }
    }

    protected pickRank(spell: Spell, index: number): void {
        this.pickedRanks.update((picked) => ({ ...picked, [spell.name]: index }));
    }

    protected rankButton(rank: SpellRank, index: number): string {
        return /\d+/.exec(rank.label)?.[0] ?? String(index + 1);
    }

    protected classicBadge(rank: SpellRank): string | null {
        return rank.classic ? (CLASSIC_BADGES[rank.classic] ?? null) : null;
    }
}
