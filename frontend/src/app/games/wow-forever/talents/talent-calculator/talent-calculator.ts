/**
 * TalentCalculator
 *
 * Talent calculator of WoW Forever: pick a class, set the level and spend points in its three trees. The talents of a
 * class are downloaded when it is picked. The build lives in the URL so it can be shared.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationService } from '@core/services/notification';
import { GameDataApi } from '../../game-data/game-data-api';
import { DataCredit, GameClass } from '../../game-data/game-data-models';
import { ClassPicker } from '../../ui/class-picker/class-picker';
import { WowButton } from '../../ui/wow-button/wow-button';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon } from '../../wow-forever-config';
import { TalentGrid } from '../talent-grid/talent-grid';
import { TalentTooltip } from '../talent-tooltip/talent-tooltip';
import { cssUrl, gameImages } from '../../game-data/game-images';
import {
    Ranks,
    canAdd,
    canRemove,
    decodeBuild,
    encodeBuild,
    minLevelFor,
    pointsAtLevel,
    pointsInTree,
    totalPoints
} from '../talents-logic';
import { ClassTalents, Talent, TalentHover, TalentTree } from '../talents-models';

@Component({
    selector: 'app-talent-calculator',
    imports: [ClassPicker, TalentGrid, TalentTooltip, WowButton, WowPage],
    templateUrl: './talent-calculator.html',
    styleUrl: './talent-calculator.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TalentCalculator implements OnInit {

    private readonly gameData = inject(GameDataApi);
    private readonly images = gameImages();
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notificationService = inject(NotificationService);

    private readonly carousel = viewChild<ElementRef<HTMLElement>>('carousel');
    private classLoad: Subscription | null = null;

    readonly classes = signal<GameClass[]>([]);
    readonly selectedClass = signal<GameClass | null>(null);
    readonly classTalents = signal<ClassTalents | null>(null);
    private readonly indexCredit = signal<DataCredit | null>(null);
    readonly ranks = signal<Ranks>({});
    readonly level = signal(0);
    readonly loading = signal(true);
    readonly loadingTrees = signal(false);
    readonly failed = signal(false);
    readonly hover = signal<TalentHover | null>(null);
    readonly activeTree = signal(0);

    readonly trees = computed<TalentTree[]>(() => this.classTalents()?.trees ?? []);

    /** Levels that give a talent point (from the backend index): the first and the last one are the ends of the level control. */
    private readonly levels = signal<readonly number[]>([]);
    readonly firstLevel = computed(() => this.levels()[0] ?? 0);
    readonly maxLevel = computed(() => this.levels().at(-1) ?? 0);

    readonly pointsTotal = computed(() => pointsAtLevel(this.levels(), this.level()));
    readonly pointsSpent = computed(() => totalPoints(this.trees(), this.ranks()));
    readonly pointsLeft = computed(() => this.pointsTotal() - this.pointsSpent());
    readonly treePoints = computed(() => this.trees().map(tree => pointsInTree(tree, this.ranks())));
    /** The level cannot go below what the spent points need. */
    readonly minLevel = computed(() => minLevelFor(this.levels(), this.pointsSpent()));

    /** The talent under the pointer with its tree, for the tooltip card. */
    readonly hovered = computed(() => {
        const hover = this.hover();
        const tree = hover ? this.treeOf(hover.talent) : undefined;

        return hover && tree ? { ...hover, tree } : null;
    });

    readonly credit = computed(() => creditLine(this.classTalents()?.credit ?? this.indexCredit()));

    readonly onIconError = hideBrokenIcon;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.classLoad?.unsubscribe());
    }

    ngOnInit(): void {
        this.gameData.getIndex().subscribe({
            next: index => {
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
            }
        });
    }

    selectClass(gameClass: GameClass, build?: string, level?: number): void {
        this.selectedClass.set(gameClass);
        this.classTalents.set(null);
        this.ranks.set({});
        this.hover.set(null);
        this.activeTree.set(0);

        if (level) {
            this.level.set(Math.min(this.maxLevel(), Math.max(this.firstLevel(), level)));
        }

        this.classLoad?.unsubscribe();
        this.loadingTrees.set(true);
        this.classLoad = this.gameData.getTalents(gameClass.slug).subscribe({
            next: talents => {
                this.classTalents.set(talents);
                this.loadingTrees.set(false);

                if (build) {
                    this.applyBuild(build);
                }

                this.syncUrl();
            },
            error: () => {
                this.loadingTrees.set(false);
                this.notificationService.error(`No se han podido cargar los talentos de ${gameClass.name}`);
            }
        });
    }

    addPoint(talent: Talent): void {
        const tree = this.treeOf(talent);

        if (tree && canAdd(talent, tree, this.ranks(), this.pointsLeft())) {
            this.ranks.update(ranks => ({ ...ranks, [talent.id]: (ranks[talent.id] ?? 0) + 1 }));
            this.syncUrl();
        }
    }

    removePoint(talent: Talent): void {
        const tree = this.treeOf(talent);

        if (tree && canRemove(talent, tree, this.ranks())) {
            this.ranks.update(ranks => ({ ...ranks, [talent.id]: (ranks[talent.id] ?? 0) - 1 }));
            this.syncUrl();
        }
    }

    reset(): void {
        this.ranks.set({});
        this.syncUrl();
    }

    changeLevel(value: number | string): void {
        const parsed = Number(value);

        if (!Number.isFinite(parsed)) {
            return;
        }

        this.level.set(Math.min(this.maxLevel(), Math.max(this.minLevel(), Math.round(parsed))));
        this.syncUrl();
    }

    share(): void {
        const url = `${window.location.origin}${this.router.url}`;

        navigator.clipboard.writeText(url).then(
            () => this.notificationService.success('Enlace de la build copiado'),
            () => this.notificationService.error('No se ha podido copiar el enlace')
        );
    }

    onHover(hover: TalentHover | null): void {
        this.hover.set(hover);
    }

    treeBackground(tree: TalentTree): string {
        return cssUrl(this.images()[`talents/${this.selectedClass()?.slug}-${tree.slug}`]);
    }

    onCarouselScroll(): void {
        const element = this.carousel()?.nativeElement;

        if (element && element.clientWidth > 0) {
            this.activeTree.set(Math.round(element.scrollLeft / element.clientWidth));
        }
    }

    goToTree(index: number): void {
        const element = this.carousel()?.nativeElement;

        this.activeTree.set(index);
        element?.scrollTo({ left: index * element.clientWidth, behavior: 'smooth' });
    }

    private treeOf(talent: Talent): TalentTree | undefined {
        return this.trees().find(tree => tree.id === talent.treeId);
    }

    private applyBuild(build: string): void {
        const decoded = decodeBuild(this.trees(), build);
        const spent = totalPoints(this.trees(), decoded);

        if (spent === 0) {
            return;
        }

        // The shared build may use more points than the level in the link allows.
        this.level.update(level => Math.max(level, minLevelFor(this.levels(), spent)));
        this.ranks.set(decoded);
    }

    private openFromUrl(): void {
        const params = this.route.snapshot.queryParamMap;
        const classes = this.classes();
        const fromUrl = classes.find(gameClass => gameClass.slug === params.get('class')) ?? classes[0];

        if (fromUrl) {
            this.selectClass(fromUrl, params.get('build') ?? undefined, Number(params.get('level')) || undefined);
        }
    }

    private syncUrl(): void {
        const gameClass = this.selectedClass();

        if (!gameClass) {
            return;
        }

        const build = this.pointsSpent() > 0 ? encodeBuild(this.trees(), this.ranks()) : null;

        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { class: gameClass.slug, level: this.level(), build },
            replaceUrl: true
        });
    }
}
