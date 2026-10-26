/**
 * LegacyPlanner
 *
 * Legacy System of WoW Forever: the three perk trees, where a character spends its Legacy points (same rules as the
 * talent trees, with tree-wide point gates), and the account challenges that earn the points. The perk build lives in
 * the URL so it can be shared; the ticked challenges are kept in this browser.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '@core/services/notification';
import { GameDataApi } from '../../game-data/game-data-api';
import { TalentGrid } from '../../talents/talent-grid/talent-grid';
import { TalentTooltip } from '../../talents/talent-tooltip/talent-tooltip';
import { Ranks, canAdd, canRemove, decodeBuild, encodeBuild, pointsInTree, totalPoints } from '../../talents/talents-logic';
import { Talent, TalentHover, TalentTree } from '../../talents/talents-models';
import { WowButton } from '../../ui/wow-button/wow-button';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon, wowIconUrl } from '../../wow-forever-config';
import { CHALLENGE_GROUP_LABELS, COMPLETED_CHALLENGES_KEY } from '../legacy-config';
import { ChallengeGroup, LegacyData } from '../legacy-models';

/** Reads the ticked challenges. Storage can be blocked or hold anything, so whatever fails reads as nothing ticked. */
const readCompleted = (): Set<string> => {
    try {
        const parsed: unknown = JSON.parse(localStorage.getItem(COMPLETED_CHALLENGES_KEY) ?? '[]');

        return new Set(Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []);
    } catch {
        return new Set();
    }
};

const writeCompleted = (completed: ReadonlySet<string>): void => {
    try {
        localStorage.setItem(COMPLETED_CHALLENGES_KEY, JSON.stringify([...completed]));
    } catch {
        // Without storage the ticks still work for this visit.
    }
};

@Component({
    selector: 'app-legacy-planner',
    imports: [TalentGrid, TalentTooltip, WowButton, WowPage],
    templateUrl: './legacy-planner.html',
    styleUrl: './legacy-planner.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class LegacyPlanner implements OnInit {

    private readonly gameData = inject(GameDataApi);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notificationService = inject(NotificationService);

    private readonly carousel = viewChild<ElementRef<HTMLElement>>('carousel');

    readonly data = signal<LegacyData | null>(null);
    readonly ranks = signal<Ranks>({});
    readonly loading = signal(true);
    readonly failed = signal(false);
    readonly hover = signal<TalentHover | null>(null);
    readonly activeTree = signal(0);
    readonly completed = signal<ReadonlySet<string>>(readCompleted());

    readonly trees = computed<TalentTree[]>(() => this.data()?.trees ?? []);
    readonly pointsTotal = computed(() => this.data()?.points ?? 0);
    readonly pointsSpent = computed(() => totalPoints(this.trees(), this.ranks()));
    readonly pointsLeft = computed(() => this.pointsTotal() - this.pointsSpent());
    readonly treePoints = computed(() => this.trees().map(tree => pointsInTree(tree, this.ranks())));

    readonly groups = computed<ChallengeGroup[]>(() => this.data()?.challengeGroups ?? []);
    readonly challengeCount = computed(() => this.groups().reduce((sum, group) => sum + group.challenges.length, 0));
    readonly completedCount = computed(() => this.groups().reduce((sum, group) => sum + this.completedIn(group), 0));
    readonly earnedPoints = computed(() =>
        this.groups().reduce((sum, group) => sum + this.pointsEarnedIn(group), 0));

    readonly hovered = computed(() => {
        const hover = this.hover();
        const tree = hover ? this.treeOf(hover.talent) : undefined;

        return hover && tree ? { ...hover, tree } : null;
    });

    readonly credit = computed(() => creditLine(this.data()?.credit ?? null));

    readonly iconUrl = wowIconUrl;
    readonly onIconError = hideBrokenIcon;

    ngOnInit(): void {
        this.gameData.getLegacy().subscribe({
            next: data => {
                this.data.set(data);
                this.loading.set(false);
                this.applyBuild(this.route.snapshot.queryParamMap.get('build'));
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            }
        });
    }

    /* ─── Perks ─── */

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

    share(): void {
        const url = `${window.location.origin}${this.router.url}`;

        navigator.clipboard.writeText(url).then(
            () => this.notificationService.success('Enlace de los perks copiado'),
            () => this.notificationService.error('No se ha podido copiar el enlace')
        );
    }

    onHover(hover: TalentHover | null): void {
        this.hover.set(hover);
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

    /* ─── Challenges ─── */

    groupLabel(group: ChallengeGroup): string {
        return CHALLENGE_GROUP_LABELS[group.name] ?? group.name;
    }

    isCompleted(name: string): boolean {
        return this.completed().has(name);
    }

    toggleChallenge(name: string): void {
        const next = new Set(this.completed());

        if (next.has(name)) {
            next.delete(name);
        } else {
            next.add(name);
        }

        this.completed.set(next);
        writeCompleted(next);
    }

    completedIn(group: ChallengeGroup): number {
        return group.challenges.filter(challenge => this.isCompleted(challenge.name)).length;
    }

    pointsEarnedIn(group: ChallengeGroup): number {
        return group.challenges.reduce((sum, challenge) => sum + (this.isCompleted(challenge.name) ? challenge.points : 0), 0);
    }

    /* ─── Private ─── */

    private treeOf(talent: Talent): TalentTree | undefined {
        return this.trees().find(tree => tree.id === talent.treeId);
    }

    /** A shared build that needs more points than a character has is ignored. */
    private applyBuild(build: string | null): void {
        if (!build) {
            return;
        }

        const decoded = decodeBuild(this.trees(), build);

        if (totalPoints(this.trees(), decoded) <= this.pointsTotal()) {
            this.ranks.set(decoded);
        }
    }

    private syncUrl(): void {
        const build = this.pointsSpent() > 0 ? encodeBuild(this.trees(), this.ranks()) : null;

        this.router.navigate([], { relativeTo: this.route, queryParams: { build }, replaceUrl: true });
    }
}
