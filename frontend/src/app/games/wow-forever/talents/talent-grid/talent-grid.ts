/**
 * TalentGrid
 *
 * Grid of one talent tree (or Legacy perk tree): icons, rank counters and prerequisite arrows. Click adds a point; right
 * click or a long press removes one.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, output } from '@angular/core';
import { hideBrokenIcon } from '../../wow-forever-config';
import { Ranks, TalentState, prerequisitesOf, talentState } from '../talents-logic';
import { Talent, TalentHover, TalentTree } from '../talents-models';

interface TalentView {
    talent: Talent;
    rank: number;
    state: TalentState;
}

interface Arrow {
    id: string;
    path: string;
    active: boolean;
}

const PADDING = 0.12;
const LONG_PRESS_MS = 450;
const CONTEXT_MENU_GUARD_MS = 800;

@Component({
    selector: 'app-talent-grid',
    templateUrl: './talent-grid.html',
    styleUrl: './talent-grid.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TalentGrid {
    readonly tree = input.required<TalentTree>();
    readonly ranks = input<Ranks>({});
    readonly pointsLeft = input(0);

    readonly add = output<Talent>();
    readonly remove = output<Talent>();
    readonly hover = output<TalentHover | null>();

    protected readonly views = computed<TalentView[]>(() => {
        const tree = this.tree();
        const ranks = this.ranks();

        return tree.talents.map((talent) => ({
            talent,
            rank: ranks[talent.id] ?? 0,
            state: talentState(talent, tree, ranks, this.pointsLeft()),
        }));
    });

    protected readonly arrows = computed<Arrow[]>(() => {
        const tree = this.tree();
        const ranks = this.ranks();

        return tree.talents.flatMap((talent) =>
            prerequisitesOf(tree, talent).map((required) => ({
                id: `${required.id}-${talent.id}`,
                path: this.pathBetween(required, talent),
                active: (ranks[required.id] ?? 0) >= required.maxRanks,
            })),
        );
    });

    protected readonly hideBrokenIcon = hideBrokenIcon;

    private pressTimer: ReturnType<typeof setTimeout> | null = null;
    private suppressClick = false;
    private lastLongPress = 0;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.cancelPress());
    }

    protected showTooltipAtPointer(event: MouseEvent, talent: Talent): void {
        this.hover.emit({ talent, x: event.clientX, y: event.clientY });
    }

    protected showTooltipBelow(event: FocusEvent, talent: Talent): void {
        const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();

        this.hover.emit({ talent, x: rect.left + rect.width / 2, y: rect.bottom });
    }

    protected addPoint(talent: Talent): void {
        if (this.suppressClick) {
            this.suppressClick = false;
            return;
        }

        this.add.emit(talent);
    }

    protected removePointFromMenu(event: MouseEvent, talent: Talent): void {
        event.preventDefault();

        if (Date.now() - this.lastLongPress > CONTEXT_MENU_GUARD_MS) {
            this.remove.emit(talent);
        }
    }

    protected startLongPress(event: PointerEvent, talent: Talent): void {
        if (event.pointerType !== 'touch') {
            return;
        }

        this.cancelPress();
        this.pressTimer = setTimeout(() => {
            this.pressTimer = null;
            this.suppressClick = true;
            this.lastLongPress = Date.now();
            this.remove.emit(talent);
        }, LONG_PRESS_MS);
    }

    protected cancelPress(): void {
        if (this.pressTimer !== null) {
            clearTimeout(this.pressTimer);
            this.pressTimer = null;
        }
    }

    protected removeOnDeleteKey(event: KeyboardEvent, talent: Talent): void {
        if (event.key === 'Backspace' || event.key === 'Delete') {
            event.preventDefault();
            this.remove.emit(talent);
        }
    }

    private pathBetween(from: Talent, to: Talent): string {
        const fromX = from.column + 0.5;
        const fromBottom = from.row + 1 - PADDING;
        const toTop = to.row + PADDING;

        if (from.column === to.column) {
            return `M ${fromX} ${fromBottom} V ${toTop}`;
        }

        const toY = to.row + 0.5;
        const entersFromLeft = to.column > from.column;
        const toEdge = entersFromLeft ? to.column + PADDING : to.column + 1 - PADDING;

        if (from.row === to.row) {
            const fromEdge = entersFromLeft ? from.column + 1 - PADDING : from.column + PADDING;

            return `M ${fromEdge} ${toY} H ${toEdge}`;
        }

        return `M ${fromX} ${fromBottom} V ${toY} H ${toEdge}`;
    }
}
