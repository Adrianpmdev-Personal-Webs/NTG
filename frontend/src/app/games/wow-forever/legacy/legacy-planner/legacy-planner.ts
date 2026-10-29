/**
 * LegacyPlanner
 *
 * Legacy System of WoW Forever: the three perk trees, where a character spends its Legacy points (same rules as the
 * talent trees, with tree-wide point gates), and the account challenges that earn the points, one folding section per
 * group with its progress. The perk build lives in the URL so it can be shared; the ticked challenges are kept in this
 * browser.
 *
 * @author @adrianpmdev
 */

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    OnInit,
    computed,
    inject,
    signal,
    viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationCenter } from '@core/notifications/notification-center';
import { GameDataApi } from '../../game-data/game-data-api';
import { TalentGrid } from '../../talents/talent-grid/talent-grid';
import { TalentTooltip } from '../../talents/talent-tooltip/talent-tooltip';
import {
    Ranks,
    canAdd,
    canRemove,
    decodeBuild,
    encodeBuild,
    pointsInTree,
    totalPoints,
} from '../../talents/talents-logic';
import { Talent, TalentHover, TalentTree } from '../../talents/talents-models';
import { WowButton } from '../../ui/wow-button/wow-button';
import { WowPage } from '../../ui/wow-page/wow-page';
import { creditLine, hideBrokenIcon } from '../../wow-forever-config';
import { COMPLETED_CHALLENGES_KEY } from '../legacy-config';
import { ChallengeGroup, LegacyData } from '../legacy-models';

const readCompleted = (): Set<string> => {
    try {
        const parsed: unknown = JSON.parse(localStorage.getItem(COMPLETED_CHALLENGES_KEY) ?? '[]');

        return new Set(Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []);
    } catch {
        return new Set();
    }
};

const writeCompleted = (completed: ReadonlySet<string>): boolean => {
    try {
        localStorage.setItem(COMPLETED_CHALLENGES_KEY, JSON.stringify([...completed]));
        return true;
    } catch {
        return false;
    }
};

@Component({
    selector: 'app-legacy-planner',
    imports: [TalentGrid, TalentTooltip, WowButton, WowPage],
    templateUrl: './legacy-planner.html',
    styleUrl: './legacy-planner.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegacyPlanner implements OnInit {
    private readonly gameData = inject(GameDataApi);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notificationCenter = inject(NotificationCenter);

    private readonly carousel = viewChild<ElementRef<HTMLElement>>('carousel');

    private readonly data = signal<LegacyData | null>(null);
    protected readonly ranks = signal<Ranks>({});
    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly hover = signal<TalentHover | null>(null);
    protected readonly activeTree = signal(0);
    private readonly completed = signal<ReadonlySet<string>>(readCompleted());

    protected readonly trees = computed<TalentTree[]>(() => this.data()?.trees ?? []);
    protected readonly pointsTotal = computed(() => this.data()?.points ?? 0);
    protected readonly pointsSpent = computed(() => totalPoints(this.trees(), this.ranks()));
    protected readonly pointsLeft = computed(() => this.pointsTotal() - this.pointsSpent());
    protected readonly treePoints = computed(() => this.trees().map((tree) => pointsInTree(tree, this.ranks())));

    protected readonly groups = computed<ChallengeGroup[]>(() => this.data()?.challengeGroups ?? []);
    protected readonly challengeCount = computed(() =>
        this.groups().reduce((sum, group) => sum + group.challenges.length, 0),
    );
    protected readonly completedCount = computed(() =>
        this.groups().reduce((sum, group) => sum + this.completedIn(group), 0),
    );
    protected readonly earnedPoints = computed(() =>
        this.groups().reduce((sum, group) => sum + this.pointsEarnedIn(group), 0),
    );

    protected readonly hovered = computed(() => {
        const hover = this.hover();
        const tree = hover ? this.treeOf(hover.talent) : undefined;

        return hover && tree ? { ...hover, tree } : null;
    });

    protected readonly credit = computed(() => creditLine(this.data()?.credit ?? null));

    protected readonly hideBrokenIcon = hideBrokenIcon;

    ngOnInit(): void {
        this.gameData.getLegacy().subscribe({
            next: (data) => {
                this.data.set(data);
                this.loading.set(false);
                this.applyBuild(this.route.snapshot.queryParamMap.get('build'));
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected addPoint(talent: Talent): void {
        const tree = this.treeOf(talent);

        if (tree && canAdd(talent, tree, this.ranks(), this.pointsLeft())) {
            this.ranks.update((ranks) => ({ ...ranks, [talent.id]: (ranks[talent.id] ?? 0) + 1 }));
            this.syncUrl();
        }
    }

    protected removePoint(talent: Talent): void {
        const tree = this.treeOf(talent);

        if (tree && canRemove(talent, tree, this.ranks())) {
            this.ranks.update((ranks) => ({ ...ranks, [talent.id]: (ranks[talent.id] ?? 0) - 1 }));
            this.syncUrl();
        }
    }

    protected reset(): void {
        this.ranks.set({});
        this.syncUrl();
    }

    protected share(): void {
        const url = `${window.location.origin}${this.router.url}`;

        navigator.clipboard.writeText(url).then(
            () => this.notificationCenter.success('Enlace de los perks copiado'),
            () => this.notificationCenter.error('No se ha podido copiar el enlace'),
        );
    }

    protected setHoveredTalent(hover: TalentHover | null): void {
        this.hover.set(hover);
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

    protected isCompleted(name: string): boolean {
        return this.completed().has(name);
    }

    protected toggleChallenge(name: string): void {
        const next = new Set(this.completed());

        if (next.has(name)) {
            next.delete(name);
        } else {
            next.add(name);
        }

        this.completed.set(next);
        writeCompleted(next);
    }

    protected completedIn(group: ChallengeGroup): number {
        return group.challenges.filter((challenge) => this.isCompleted(challenge.name)).length;
    }

    protected pointsEarnedIn(group: ChallengeGroup): number {
        return group.challenges.reduce(
            (sum, challenge) => sum + (this.isCompleted(challenge.name) ? challenge.points : 0),
            0,
        );
    }

    protected progressOf(group: ChallengeGroup): number {
        return group.challenges.length ? (this.completedIn(group) * 100) / group.challenges.length : 0;
    }

    private treeOf(talent: Talent): TalentTree | undefined {
        return this.trees().find((tree) => tree.id === talent.treeId);
    }

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
