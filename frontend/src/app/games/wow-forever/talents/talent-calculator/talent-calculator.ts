/**
 * TalentCalculator
 *
 * Talent calculator of WoW Forever: pick a class, set the level and spend points in its three trees, seeing the level
 * the spent points need. The talents of a class are downloaded when it is picked. The build lives in the URL so it can
 * be shared, and the player can keep builds of each class in slots of their account. Every point is recorded in the
 * order it was spent, so the build can be followed level by level: a point can only be removed when the points spent
 * after it would still have been allowed, and undo (also Ctrl+Z) takes back the last one. A build opened without its
 * order keeps working as before and asks to be made again to record it.
 *
 * @author @adrianpmdev
 */

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    OnInit,
    computed,
    inject,
    signal,
    viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { GameDataApi } from '../../game-data/game-data-api';
import { DataCredit, GameClass } from '../../game-data/game-data-models';
import { ClassPicker } from '../../ui/class-picker/class-picker';
import { WowButton } from '../../ui/wow-button/wow-button';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon } from '../../wow-forever-config';
import { SavedBuilds } from '../saved-builds/saved-builds';
import { SavedBuild } from '../saved-builds-models';
import { TalentGrid } from '../talent-grid/talent-grid';
import { TalentOrder } from '../talent-order/talent-order';
import { TalentTooltip } from '../talent-tooltip/talent-tooltip';
import { cssUrl, gameImages } from '../../game-data/game-images';
import {
    PointOrder,
    Ranks,
    canAdd,
    canRemove,
    canRemoveFromOrder,
    decodeBuild,
    decodeOrder,
    encodeBuild,
    encodeOrder,
    minLevelFor,
    pointsAtLevel,
    pointsInTree,
    ranksFromOrder,
    sameRanks,
    totalPoints,
    withoutLastPoint,
} from '../talents-logic';
import { ClassTalents, Talent, TalentHover, TalentTree } from '../talents-models';

@Component({
    selector: 'app-talent-calculator',
    imports: [ClassPicker, SavedBuilds, TalentGrid, TalentOrder, TalentTooltip, WowButton, WowPage],
    templateUrl: './talent-calculator.html',
    styleUrl: './talent-calculator.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { '(document:keydown)': 'undoFromKeyboard($event)' },
})
export class TalentCalculator implements OnInit {
    private readonly gameData = inject(GameDataApi);
    private readonly images = gameImages();
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notificationCenter = inject(NotificationCenter);

    private readonly carousel = viewChild<ElementRef<HTMLElement>>('carousel');
    private classLoad: Subscription | null = null;

    protected readonly classes = signal<GameClass[]>([]);
    protected readonly selectedClass = signal<GameClass | null>(null);
    private readonly classTalents = signal<ClassTalents | null>(null);
    private readonly indexCredit = signal<DataCredit | null>(null);
    protected readonly order = signal<PointOrder | null>([]);
    private readonly unorderedRanks = signal<Ranks>({});
    protected readonly ranks = computed<Ranks>(() => {
        const order = this.order();

        return order ? ranksFromOrder(order) : this.unorderedRanks();
    });
    protected readonly level = signal(0);
    protected readonly loading = signal(true);
    protected readonly loadingTrees = signal(false);
    protected readonly failed = signal(false);
    protected readonly hover = signal<TalentHover | null>(null);
    protected readonly activeTree = signal(0);

    protected readonly trees = computed<TalentTree[]>(() => this.classTalents()?.trees ?? []);

    protected readonly levels = signal<readonly number[]>([]);
    protected readonly firstLevel = computed(() => this.levels()[0] ?? 0);
    protected readonly maxLevel = computed(() => this.levels().at(-1) ?? 0);

    protected readonly pointsTotal = computed(() => pointsAtLevel(this.levels(), this.level()));
    protected readonly pointsSpent = computed(() => totalPoints(this.trees(), this.ranks()));
    protected readonly pointsLeft = computed(() => this.pointsTotal() - this.pointsSpent());
    protected readonly treePoints = computed(() => this.trees().map((tree) => pointsInTree(tree, this.ranks())));
    protected readonly minLevel = computed(() => minLevelFor(this.levels(), this.pointsSpent()));
    protected readonly currentBuild = computed(() =>
        this.pointsSpent() > 0 ? encodeBuild(this.trees(), this.ranks()) : null,
    );
    protected readonly currentOrder = computed(() => {
        const order = this.order();

        return order && order.length > 0 ? encodeOrder(this.trees(), order) : null;
    });
    protected readonly canUndo = computed(() => (this.order()?.length ?? 0) > 0);

    protected readonly hovered = computed(() => {
        const hover = this.hover();
        const tree = hover ? this.treeOf(hover.talent) : undefined;

        return hover && tree ? { ...hover, tree } : null;
    });

    protected readonly credit = computed(() => creditLine(this.classTalents()?.credit ?? this.indexCredit()));

    protected readonly hideBrokenIcon = hideBrokenIcon;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.classLoad?.unsubscribe());
    }

    ngOnInit(): void {
        this.gameData.getIndex().subscribe({
            next: (index) => {
                this.classes.set(index.classes);
                this.indexCredit.set(index.credit);
                this.levels.set(index.talentPointLevels);
                this.level.set(index.talentPointLevels.at(-1) ?? 0);
                this.loading.set(false);
                this.openFromUrl();
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected selectClass(gameClass: GameClass, build?: string, level?: number, order?: string): void {
        this.selectedClass.set(gameClass);
        this.classTalents.set(null);
        this.clearPoints();
        this.hover.set(null);
        this.activeTree.set(0);

        if (level) {
            this.level.set(Math.min(this.maxLevel(), Math.max(this.firstLevel(), level)));
        }

        this.classLoad?.unsubscribe();
        this.loadingTrees.set(true);
        this.classLoad = this.gameData.getTalents(gameClass.slug).subscribe({
            next: (talents) => {
                this.classTalents.set(talents);
                this.loadingTrees.set(false);

                if (build) {
                    this.applyBuild(build, order);
                }

                this.syncUrl();
            },
            error: () => {
                this.loadingTrees.set(false);
                this.notificationCenter.error(`No se han podido cargar los talentos de ${gameClass.name}`);
            },
        });
    }

    protected addPoint(talent: Talent): void {
        const tree = this.treeOf(talent);

        if (!tree || !canAdd(talent, tree, this.ranks(), this.pointsLeft())) {
            return;
        }

        if (this.order()) {
            this.order.update((order) => [...(order ?? []), talent.id]);
        } else {
            this.unorderedRanks.update((ranks) => ({ ...ranks, [talent.id]: (ranks[talent.id] ?? 0) + 1 }));
        }

        this.syncUrl();
    }

    protected removePoint(talent: Talent): void {
        const tree = this.treeOf(talent);
        const order = this.order();

        if (order && canRemoveFromOrder(talent, this.trees(), order)) {
            this.order.set(withoutLastPoint(order, talent));
            this.syncUrl();
        } else if (!order && tree && canRemove(talent, tree, this.ranks())) {
            this.unorderedRanks.update((ranks) => ({ ...ranks, [talent.id]: (ranks[talent.id] ?? 0) - 1 }));
            this.syncUrl();
        }
    }

    protected undo(): void {
        if (this.canUndo()) {
            this.order.update((order) => order?.slice(0, -1) ?? null);
            this.syncUrl();
        }
    }

    protected undoFromKeyboard(event: KeyboardEvent): void {
        const target = event.target as HTMLElement | null;
        const typing = target?.isContentEditable || ['INPUT', 'SELECT', 'TEXTAREA'].includes(target?.tagName ?? '');

        if (!(event.ctrlKey || event.metaKey) || event.shiftKey || event.code !== 'KeyZ' || typing || !this.canUndo()) {
            return;
        }

        event.preventDefault();
        this.undo();
    }

    protected reset(): void {
        this.clearPoints();
        this.syncUrl();
    }

    protected changeLevel(value: number | string): void {
        const parsed = Number(value);

        if (!Number.isFinite(parsed)) {
            return;
        }

        this.level.set(Math.min(this.maxLevel(), Math.max(this.minLevel(), Math.round(parsed))));
        this.syncUrl();
    }

    protected loadSavedBuild(saved: SavedBuild): void {
        this.level.set(Math.min(this.maxLevel(), Math.max(this.firstLevel(), saved.level)));
        this.applyBuild(saved.build, saved.order ?? undefined);
        this.syncUrl();
    }

    protected share(): void {
        const url = `${window.location.origin}${this.router.url}`;

        navigator.clipboard.writeText(url).then(
            () => this.notificationCenter.success('Enlace de la build copiado'),
            () => this.notificationCenter.error('No se ha podido copiar el enlace'),
        );
    }

    protected setHoveredTalent(hover: TalentHover | null): void {
        this.hover.set(hover);
    }

    protected treeBackground(tree: TalentTree): string {
        return cssUrl(this.images()[`talents/${this.selectedClass()?.slug}-${tree.slug}`]);
    }

    protected trackCarouselPosition(): void {
        const element = this.carousel()?.nativeElement;

        if (element && element.clientWidth > 0) {
            this.activeTree.set(Math.round(element.scrollLeft / element.clientWidth));
        }
    }

    protected goToTree(index: number): void {
        const element = this.carousel()?.nativeElement;

        this.activeTree.set(index);
        element?.scrollTo({ left: index * element.clientWidth, behavior: 'smooth' });
    }

    private treeOf(talent: Talent): TalentTree | undefined {
        return this.trees().find((tree) => tree.id === talent.treeId);
    }

    private clearPoints(): void {
        this.order.set([]);
        this.unorderedRanks.set({});
    }

    private applyBuild(build: string, orderCode?: string): void {
        const decoded = decodeBuild(this.trees(), build);
        const spent = totalPoints(this.trees(), decoded);
        const order = orderCode ? decodeOrder(this.trees(), orderCode) : null;

        if (spent === 0) {
            return;
        }

        this.level.update((level) => Math.max(level, minLevelFor(this.levels(), spent)));

        if (order && sameRanks(ranksFromOrder(order), decoded)) {
            this.order.set(order);
            this.unorderedRanks.set({});
        } else {
            this.order.set(null);
            this.unorderedRanks.set(decoded);
        }
    }

    private openFromUrl(): void {
        const params = this.route.snapshot.queryParamMap;
        const classes = this.classes();
        const fromUrl = classes.find((gameClass) => gameClass.slug === params.get('class')) ?? classes[0];

        if (fromUrl) {
            this.selectClass(
                fromUrl,
                params.get('build') ?? undefined,
                Number(params.get('level')) || undefined,
                params.get('order') ?? undefined,
            );
        }
    }

    private syncUrl(): void {
        const gameClass = this.selectedClass();

        if (!gameClass) {
            return;
        }

        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {
                class: gameClass.slug,
                level: this.level(),
                build: this.currentBuild(),
                order: this.currentOrder(),
            },
            replaceUrl: true,
        });
    }
}
