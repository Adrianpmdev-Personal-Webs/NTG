/**
 * TalentCalculator
 *
 * Talent calculator of WoW Forever: pick a class, set the level and spend points in its three trees. The build lives in the URL so it can be shared.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { NotificationService } from '@core/services/notification';
import { WowForeverApi } from '../../wow-forever-api';
import { TREE_ICONS, hideBrokenIcon, talentIconUrl, wowIconUrl } from '../../wow-forever-config';
import { WowClass } from '../../wow-forever-models';
import { WowButton } from '../../ui/wow-button/wow-button';
import { WowFrame } from '../../ui/wow-frame/wow-frame';
import { TalentGrid } from '../talent-grid/talent-grid';
import {
    Ranks,
    canAdd,
    canRemove,
    decodeBuild,
    encodeBuild,
    minLevelFor,
    pointsAboveRow,
    pointsAtLevel,
    pointsInTree,
    prerequisitesOf,
    rankText,
    totalPoints,
    treeSlug
} from '../talents-logic';
import { Talent, TalentHover, TalentTree, TalentsData } from '../talents-models';
import { TalentsApi } from '../talents-api';

interface TalentTooltip {
    talent: Talent;
    tree: TalentTree;
    rank: number;
    current: string | null;
    next: string | null;
    requirements: string[];
}

/** One background picture per tree, named <class>-<tree>.jpg (e.g. warrior-arms.jpg). */
const TREE_ART_PATH = '/images/wow-forever/talents/trees';
/** Distance between the pointer and the tooltip card. */
const TOOLTIP_OFFSET = 16;

@Component({
    selector: 'app-talent-calculator',
    imports: [TalentGrid, WowButton, WowFrame],
    templateUrl: './talent-calculator.html',
    styleUrl: './talent-calculator.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TalentCalculator implements OnInit {

    private readonly wowApi = inject(WowForeverApi);
    private readonly talentsApi = inject(TalentsApi);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notificationService = inject(NotificationService);

    private readonly carousel = viewChild<ElementRef<HTMLElement>>('carousel');

    readonly classes = signal<WowClass[]>([]);
    readonly data = signal<TalentsData | null>(null);
    readonly selectedClass = signal<WowClass | null>(null);
    readonly ranks = signal<Ranks>({});
    readonly level = signal(0);
    readonly loading = signal(true);
    readonly failed = signal(false);
    readonly hovered = signal<Talent | null>(null);
    private readonly pointer = signal({ x: 0, y: 0 });
    readonly activeTree = signal(0);

    readonly trees = computed<TalentTree[]>(() => {
        const slug = this.selectedClass()?.slug;

        return this.data()?.classes.find(talentClass => talentClass.slug === slug)?.trees ?? [];
    });

    /** Levels that give a talent point: the first and the last one are the ends of the level control. */
    private readonly levels = computed(() => this.data()?.levels ?? []);
    readonly firstLevel = computed(() => this.levels()[0] ?? 0);
    readonly maxLevel = computed(() => this.levels().at(-1) ?? 0);

    readonly pointsTotal = computed(() => pointsAtLevel(this.levels(), this.level()));
    readonly pointsSpent = computed(() => totalPoints(this.trees(), this.ranks()));
    readonly pointsLeft = computed(() => this.pointsTotal() - this.pointsSpent());
    readonly treePoints = computed(() => this.trees().map(tree => pointsInTree(tree, this.ranks())));
    /** The level cannot go below what the spent points need. */
    readonly minLevel = computed(() => minLevelFor(this.levels(), this.pointsSpent()));

    readonly tooltip = computed<TalentTooltip | null>(() => {
        const talent = this.hovered();
        const tree = talent ? this.treeOf(talent) : undefined;

        if (!talent || !tree) {
            return null;
        }

        const ranks = this.ranks();
        const rank = ranks[talent.id] ?? 0;
        const requirements: string[] = [];

        if (pointsAboveRow(tree, ranks, talent.row) < talent.pointsRequired) {
            requirements.push(`Requiere ${talent.pointsRequired} puntos en ${tree.name}`);
        }

        const prerequisites = prerequisitesOf(tree, talent);

        if (prerequisites.length > 0 && !prerequisites.some(required => (ranks[required.id] ?? 0) >= required.maxRanks)) {
            const options = prerequisites.map(required =>
                `${required.maxRanks} ${required.maxRanks === 1 ? 'punto' : 'puntos'} en ${required.name}`);

            requirements.push(`Requiere ${options.join(' o ')}`);
        }

        return {
            talent,
            tree,
            rank,
            current: rank > 0 ? rankText(talent, rank) : null,
            next: rank < talent.maxRanks ? rankText(talent, rank + 1) : null,
            requirements
        };
    });

    /** The card hangs from the pointer, on the side of it that has more room, so it never leaves the screen. */
    readonly tooltipStyle = computed<Record<string, string>>(() => {
        const { x, y } = this.pointer();

        return {
            ...(x > window.innerWidth / 2
                ? { right: `${window.innerWidth - x + TOOLTIP_OFFSET}px` }
                : { left: `${x + TOOLTIP_OFFSET}px` }),
            ...(y > window.innerHeight / 2
                ? { bottom: `${window.innerHeight - y + TOOLTIP_OFFSET}px` }
                : { top: `${y + TOOLTIP_OFFSET}px` })
        };
    });

    readonly iconUrl = wowIconUrl;
    readonly talentIconUrl = talentIconUrl;
    readonly onIconError = hideBrokenIcon;

    ngOnInit(): void {
        forkJoin({ classes: this.wowApi.getClasses(), data: this.talentsApi.getTalents() }).subscribe({
            next: ({ classes, data }) => {
                this.classes.set(classes);
                this.data.set(data);
                this.level.set(this.maxLevel());
                this.loading.set(false);
                this.openFromUrl();
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            }
        });
    }

    selectClass(wowClass: WowClass, build?: string, level?: number): void {
        this.selectedClass.set(wowClass);
        this.ranks.set({});
        this.hovered.set(null);
        this.activeTree.set(0);

        if (level) {
            this.level.set(Math.min(this.maxLevel(), Math.max(this.firstLevel(), level)));
        }

        if (build) {
            this.applyBuild(build);
        }

        this.syncUrl();
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
        this.hovered.set(hover?.talent ?? null);

        if (hover) {
            this.pointer.set({ x: hover.x, y: hover.y });
        }
    }

    /** Icon of a tree (the small picture next to its name), or an empty string when the tree has none. */
    treeIcon(tree: TalentTree): string {
        const icon = TREE_ICONS[this.treeKey(tree)];

        return icon ? wowIconUrl(icon) : '';
    }

    treeBackground(tree: TalentTree): string {
        return `url('${TREE_ART_PATH}/${this.treeKey(tree)}.jpg')`;
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

    /** Name of a tree in file names and in the icon table, e.g. `warrior-arms`. */
    private treeKey(tree: TalentTree): string {
        return `${this.selectedClass()?.slug}-${treeSlug(tree)}`;
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
        const fromUrl = classes.find(wowClass => wowClass.slug === params.get('class')) ?? classes[0];

        if (fromUrl) {
            this.selectClass(fromUrl, params.get('build') ?? undefined, Number(params.get('level')) || undefined);
        }
    }

    private syncUrl(): void {
        const wowClass = this.selectedClass();

        if (!wowClass) {
            return;
        }

        const build = this.pointsSpent() > 0 ? encodeBuild(this.trees(), this.ranks()) : null;

        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { class: wowClass.slug, level: this.level(), build },
            replaceUrl: true
        });
    }
}
