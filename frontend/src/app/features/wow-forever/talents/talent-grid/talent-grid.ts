/**
 * TalentGrid
 *
 * Grid of one talent tree: icons, rank counters and prerequisite arrows. Click adds a point; right click or a long press removes one.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { hideBrokenIcon, talentIconUrl } from '../../wow-forever-config';
import { Ranks, TalentState, prerequisitesOf, talentState } from '../talents-logic';
import { Talent, TalentTree } from '../talents-models';

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

/** Space around each icon inside its cell, as a fraction of the cell. Arrows start and end at the icon edge. */
const PADDING = 0.12;
const LONG_PRESS_MS = 450;
const CONTEXT_MENU_GUARD_MS = 800;

@Component({
    selector: 'app-talent-grid',
    templateUrl: './talent-grid.html',
    styleUrl: './talent-grid.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TalentGrid {

    readonly tree = input.required<TalentTree>();
    readonly ranks = input<Ranks>({});
    readonly pointsLeft = input(0);
    readonly selectedId = input<number | null>(null);

    /** The player clicked a talent (+1 point). */
    readonly add = output<Talent>();
    /** Right click or long press (-1 point). */
    readonly remove = output<Talent>();
    readonly hover = output<Talent | null>();

    /** Only the talents the source describes are drawn. */
    private readonly visibleTalents = computed(() => this.tree().talents.filter(talent => talent.name !== null));

    readonly views = computed<TalentView[]>(() => {
        const tree = this.tree();
        const ranks = this.ranks();

        return this.visibleTalents().map(talent => ({
            talent,
            rank: ranks[talent.id] ?? 0,
            state: talentState(talent, tree, ranks, this.pointsLeft())
        }));
    });

    readonly arrows = computed<Arrow[]>(() => {
        const tree = this.tree();
        const ranks = this.ranks();

        return this.visibleTalents().flatMap(talent =>
            prerequisitesOf(tree, talent).map(required => ({
                id: `${required.id}-${talent.id}`,
                path: this.pathBetween(required, talent),
                active: (ranks[required.id] ?? 0) >= required.maxRanks
            }))
        );
    });

    readonly iconUrl = talentIconUrl;
    readonly onIconError = hideBrokenIcon;

    private pressTimer: ReturnType<typeof setTimeout> | null = null;
    private suppressClick = false;
    private lastLongPress = 0;

    onClick(talent: Talent): void {
        if (this.suppressClick) {
            this.suppressClick = false;
            return;
        }

        this.add.emit(talent);
    }

    onContextMenu(event: MouseEvent, talent: Talent): void {
        event.preventDefault();

        // A long press on touch screens can also fire this event: the long press already removed the point.
        if (Date.now() - this.lastLongPress > CONTEXT_MENU_GUARD_MS) {
            this.remove.emit(talent);
        }
    }

    onPointerDown(event: PointerEvent, talent: Talent): void {
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

    cancelPress(): void {
        if (this.pressTimer !== null) {
            clearTimeout(this.pressTimer);
            this.pressTimer = null;
        }
    }

    onKeyDown(event: KeyboardEvent, talent: Talent): void {
        if (event.key === 'Backspace' || event.key === 'Delete') {
            event.preventDefault();
            this.remove.emit(talent);
        }
    }

    /** SVG path (in cell units) from the edge of the required talent to the edge of the talent that needs it. */
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
