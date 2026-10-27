/**
 * TalentTooltip
 *
 * Card that hangs from the pointer over a talent (or Legacy perk): name, rank, cost, the text of the current and the
 * next rank, and what is still missing to unlock it. Used by the talent calculator and the Legacy planner.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { hideBrokenIcon } from '../../wow-forever-config';
import { Ranks, pointsTowardsGate, prerequisitesOf, rankText } from '../talents-logic';
import { Talent, TalentTree } from '../talents-models';

/** Distance between the pointer and the card. */
const OFFSET = 16;

/** Widest the card gets (the same as its max-width in the styles). */
const MAX_WIDTH = 320;

/** Space kept between the card and the edges of the screen. */
const EDGE = 16;

const CLASSIC_LABELS: Readonly<Partial<Record<string, string>>> = {
    new: 'Nuevo en Forever',
    changed: 'Cambiado respecto a Classic',
    moved: 'Movido de sitio respecto a Classic'
};

@Component({
    selector: 'app-talent-tooltip',
    templateUrl: './talent-tooltip.html',
    styleUrl: './talent-tooltip.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TalentTooltip {

    readonly talent = input.required<Talent>();
    readonly tree = input.required<TalentTree>();
    readonly ranks = input.required<Ranks>();
    /** Pointer position on the screen. */
    readonly x = input(0);
    readonly y = input(0);

    readonly rank = computed(() => this.ranks()[this.talent().id] ?? 0);

    readonly current = computed(() => (this.rank() > 0 ? rankText(this.talent(), this.rank()) : null));

    readonly next = computed(() =>
        this.rank() < this.talent().maxRanks ? rankText(this.talent(), this.rank() + 1) : null);

    readonly classicLabel = computed(() => {
        const classic = this.talent().classic;

        return classic ? CLASSIC_LABELS[classic] ?? null : null;
    });

    readonly requirements = computed<string[]>(() => {
        const talent = this.talent();
        const tree = this.tree();
        const ranks = this.ranks();
        const requirements: string[] = [];

        if (talent.maxRanks === 0) {
            return requirements;
        }

        if (pointsTowardsGate(tree, ranks, talent) < talent.pointsRequired) {
            requirements.push(`Requiere ${talent.pointsRequired} puntos en ${tree.name}`);
        }

        const prerequisites = prerequisitesOf(tree, talent);

        if (prerequisites.length > 0 && !prerequisites.some(required => (ranks[required.id] ?? 0) >= required.maxRanks)) {
            const options = prerequisites.map(required =>
                `${required.maxRanks} ${required.maxRanks === 1 ? 'punto' : 'puntos'} en ${required.name}`);

            requirements.push(`Requiere ${options.join(' o ')}`);
        }

        return requirements;
    });

    /**
     * The card hangs from the pointer, on the side of it that has more room. When neither side is wide enough (a phone),
     * it is centred on the screen instead, so it never leaves it.
     */
    readonly position = computed<Record<string, string>>(() => {
        const x = this.x();
        const y = this.y();
        const screenWidth = window.innerWidth;
        const width = Math.min(MAX_WIDTH, screenWidth - 2 * EDGE);
        const onLeft = x > screenWidth / 2;
        const room = (onLeft ? x : screenWidth - x) - OFFSET - EDGE;

        return {
            ...(room < width
                ? { left: `${(screenWidth - width) / 2}px` }
                : onLeft
                    ? { right: `${screenWidth - x + OFFSET}px` }
                    : { left: `${x + OFFSET}px` }),
            ...(y > window.innerHeight / 2
                ? { bottom: `${window.innerHeight - y + OFFSET}px` }
                : { top: `${y + OFFSET}px` })
        };
    });

    readonly onIconError = hideBrokenIcon;
}
