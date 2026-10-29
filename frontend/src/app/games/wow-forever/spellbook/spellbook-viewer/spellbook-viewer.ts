/**
 * SpellbookViewer
 *
 * Spellbook of each WoW Forever class, laid out like the game's: an open book with six spells on each page, a tab with
 * the icon of each talent tree at its side and pages to turn. Each slot shows the highest rank learned by the chosen
 * level, or one slot per rank with "Mostrar todos los rangos", and the tooltip of that rank from the pointer (or below
 * on focus). Spells can be filtered by name. The spellbook of a class is downloaded when it is picked; the class lives
 * in the URL so the page can be shared.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { GameDataApi } from '../../game-data/game-data-api';
import { DataCredit, GameClass } from '../../game-data/game-data-models';
import { ClassPicker } from '../../ui/class-picker/class-picker';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon } from '../../wow-forever-config';
import { SpellTooltip } from '../spell-tooltip/spell-tooltip';
import { ClassSpellbook, Spell, SpellRank } from '../spellbook-models';

interface BookSlot {
    key: string;
    spell: Spell;
    rank: SpellRank;
}

interface SlotHover {
    slot: BookSlot;
    x: number;
    y: number;
}

const MIN_LEVEL = 1;
const MAX_LEVEL = 60;
const SLOTS_PER_PAGE = 6;
const SLOTS_PER_SPREAD = SLOTS_PER_PAGE * 2;

const normalize = (text: string): string => text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

const firstLevel = (spell: Spell): number => spell.ranks.find((rank) => rank.level !== null)?.level ?? 0;

const padded = (slots: readonly BookSlot[]): (BookSlot | null)[] => [
    ...slots,
    ...Array<null>(SLOTS_PER_PAGE - slots.length).fill(null),
];

@Component({
    selector: 'app-spellbook-viewer',
    imports: [ClassPicker, SpellTooltip, WowPage],
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
    protected readonly allRanks = signal(false);
    private readonly page = signal(0);
    protected readonly hovered = signal<SlotHover | null>(null);

    protected readonly tabs = computed(() => this.spellbook()?.tabs ?? []);

    private readonly slots = computed<BookSlot[]>(() => {
        const tab = this.tabs()[this.activeTab()];
        const query = normalize(this.query());
        const limit = this.levelLimit();
        const allRanks = this.allRanks();

        if (!tab) {
            return [];
        }

        return [...tab.spells]
            .filter((spell) => !query || normalize(spell.name).includes(query))
            .sort((a, b) => firstLevel(a) - firstLevel(b))
            .flatMap((spell) => {
                const learned = spell.ranks
                    .map((rank, index) => ({ rank, key: `${spell.name}#${index}` }))
                    .filter(({ rank }) => rank.level === null || rank.level <= limit);
                const shown = allRanks ? learned : learned.slice(-1);

                return shown.map(({ rank, key }) => ({ key, spell, rank }));
            });
    });

    protected readonly pageCount = computed(() => Math.max(1, Math.ceil(this.slots().length / SLOTS_PER_SPREAD)));

    protected readonly currentPage = computed(() => Math.min(this.page(), this.pageCount() - 1));

    protected readonly pages = computed<(BookSlot | null)[][]>(() => {
        const start = this.currentPage() * SLOTS_PER_SPREAD;
        const spread = this.slots().slice(start, start + SLOTS_PER_SPREAD);

        return [padded(spread.slice(0, SLOTS_PER_PAGE)), padded(spread.slice(SLOTS_PER_PAGE))];
    });

    protected readonly empty = computed(() => this.slots().length === 0);

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
        this.turnTo(0);

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
        this.turnTo(0);
    }

    protected changeQuery(value: string): void {
        this.query.set(value);
        this.turnTo(0);
    }

    protected changeLevel(value: number | string): void {
        const parsed = Number(value);

        if (Number.isFinite(parsed)) {
            this.levelLimit.set(Math.min(MAX_LEVEL, Math.max(MIN_LEVEL, Math.round(parsed))));
            this.turnTo(0);
        }
    }

    protected toggleAllRanks(checked: boolean): void {
        this.allRanks.set(checked);
        this.turnTo(0);
    }

    protected previousPage(): void {
        this.turnTo(this.currentPage() - 1);
    }

    protected nextPage(): void {
        this.turnTo(this.currentPage() + 1);
    }

    protected showSlotAtPointer(event: MouseEvent, slot: BookSlot): void {
        this.hovered.set({ slot, x: event.clientX, y: event.clientY });
    }

    protected showSlotBelow(event: FocusEvent, slot: BookSlot): void {
        const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();

        this.hovered.set({ slot, x: rect.left + rect.width / 2, y: rect.bottom });
    }

    protected hideSlot(): void {
        this.hovered.set(null);
    }

    private turnTo(page: number): void {
        this.page.set(Math.max(0, page));
        this.hovered.set(null);
    }
}
