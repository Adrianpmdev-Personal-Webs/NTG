/**
 * SpellTooltip
 *
 * Card that hangs from the pointer over a spell of the book, as the game tooltip shows it: name and rank, the tooltip
 * lines (cost, range, cast time, cooldown), the text, and then what the site adds: school and coefficient, where it is
 * learned, notes, whether a talent gives it, how it compares with Classic and the Classic text.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ClassicStatus } from '../../game-data/game-data-models';
import { tooltipPosition } from '../../ui/tooltip-position';
import { hideBrokenIcon } from '../../wow-forever-config';
import { Spell, SpellRank } from '../spellbook-models';

const CLASSIC_LABELS: Readonly<Partial<Record<ClassicStatus, string>>> = {
    new: 'Nuevo en Forever',
    changed: 'Cambiado respecto a Classic',
};

@Component({
    selector: 'app-spell-tooltip',
    templateUrl: './spell-tooltip.html',
    styleUrl: './spell-tooltip.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpellTooltip {
    readonly spell = input.required<Spell>();
    readonly rank = input.required<SpellRank>();
    readonly x = input(0);
    readonly y = input(0);

    protected readonly position = computed(() => tooltipPosition(this.x(), this.y()));

    protected readonly classicLabel = computed(() => {
        const classic = this.rank().classic;

        return classic ? (CLASSIC_LABELS[classic] ?? null) : null;
    });

    protected readonly hideBrokenIcon = hideBrokenIcon;
}
